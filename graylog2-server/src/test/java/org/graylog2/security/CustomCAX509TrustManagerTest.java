/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.security;

import com.google.common.eventbus.EventBus;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.graylog.security.certutil.ca.exceptions.KeyStoreStorageException;
import org.graylog.security.certutil.console.TestableConsole;
import org.graylog2.bootstrap.preflight.web.resources.model.CA;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.DATANODE_KEY_ALIAS;

public class CustomCAX509TrustManagerTest {
    @TempDir
    static Path tempDir;

    static class DummyCaService implements CaService {
        private final Optional<KeyStore> keyStore;
        public DummyCaService(KeyStore keyStore) {
            this.keyStore = Optional.ofNullable(keyStore);
        }

        @Override
        public CA get() throws KeyStoreStorageException {
            return null;
        }

        @Override
        public void create(String organization, Integer daysValid, char[] password) throws CACreationException, KeyStoreStorageException {
        }

        @Override
        public void upload(String pass, List<FormDataBodyPart> parts) throws CACreationException {}

        @Override
        public void startOver() {}

        @Override
        public Optional<KeyStore> loadKeyStore() throws KeyStoreException, KeyStoreStorageException, NoSuchAlgorithmException {
            return this.keyStore;
        }
    }

    @Test
    public void testCA() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        // create certs (root, ca, datanode) copy of CertUtilTest
        final Path caPath = tempDir.resolve("test-ca.p12");
        final Path nodePath = tempDir.resolve("test-node.p12");

        final TestableConsole inputCa = TestableConsole.empty()
                .register(CertutilCa.PROMPT_ENTER_CA_PASSWORD, "password");
        final CertutilCa certutilCa = new CertutilCa(caPath.toAbsolutePath().toString(), inputCa);
        certutilCa.run();

        // now we have a ROOT + CA keypair in the keystore, let's use it to generate node keypair

        TestableConsole inputCert = TestableConsole.empty()
                .register(CertutilCert.PROMPT_ENTER_CA_PASSWORD, "password")
                .register(CertutilCert.PROMPT_ENTER_CERTIFICATE_PASSWORD, "changeme")
                .register(CertutilCert.PROMPT_ENTER_CERT_ALTERNATIVE_NAMES, "");

        CertutilCert certutilCert = new CertutilCert(
                caPath.toAbsolutePath().toString(),
                nodePath.toAbsolutePath().toString(),
                inputCert);
        certutilCert.run();

        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        caKeyStore.load(new FileInputStream(caPath.toFile()), "password".toCharArray());

        KeyStore nodeKeyStore = KeyStore.getInstance("PKCS12");
        nodeKeyStore.load(new FileInputStream(nodePath.toFile()), "changeme".toCharArray());
        final Key nodeKey = nodeKeyStore.getKey(DATANODE_KEY_ALIAS, "changeme".toCharArray());
        Assertions.assertThat(nodeKey).isNotNull();

        Assertions.assertThatCode(() -> nodeKeyStore.getCertificate(DATANODE_KEY_ALIAS).verify(caKeyStore.getCertificate(CA_KEY_ALIAS).getPublicKey()))
                .doesNotThrowAnyException();

        var hostname = Tools.getLocalCanonicalHostname();
        final Certificate[] certificateChain = nodeKeyStore.getCertificateChain(DATANODE_KEY_ALIAS);
        Assertions.assertThat(certificateChain)
                .hasSize(2)
                .extracting(c -> (X509Certificate) c)
                .extracting(c -> c.getSubjectX500Principal().getName())
                .contains("CN=Graylog CA", "CN=" + hostname);

        // additional Tests
        final var noAdditionalKeystore = new DummyCaService(null);
        final var additionalKeystore = new DummyCaService(caKeyStore);

        final EventBus serverEventBus = new EventBus();
        final var defaultTM = new CustomCAX509TrustManager(noAdditionalKeystore, serverEventBus);
        final var customTM = new CustomCAX509TrustManager(additionalKeystore, serverEventBus);

        final var default_issuers = defaultTM.getAcceptedIssuers().length;
        Assertions.assertThat(customTM.getAcceptedIssuers().length).isEqualTo(default_issuers + 1);

        final var cert = (X509Certificate) nodeKeyStore.getCertificate(DATANODE_KEY_ALIAS);

        Assertions.assertThatCode(() -> {
            try {
                defaultTM.checkClientTrusted(new X509Certificate[]{cert}, "ANY");
                throw new Exception("Should not get here");
            } catch (CertificateException e) {
                // expected
            }
        }).doesNotThrowAnyException();

        Assertions.assertThatCode(() ->
                customTM.checkClientTrusted(new X509Certificate[]{cert}, "ANY")
        ).doesNotThrowAnyException();
    }
}
