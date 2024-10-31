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
package org.graylog2.security.certutil;

import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog.security.certutil.console.TestableConsole;
import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.DATANODE_KEY_ALIAS;

class CertutilHttpTest {

    @TempDir
    static Path tempDir;

    @Test
    void testGenerateNodeCertificate() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidAlgorithmParameterException, CertPathValidatorException, SignatureException, InvalidKeyException, NoSuchProviderException {

        final Path caPath = tempDir.resolve("test-ca.p12");
        final Path nodePath = tempDir.resolve("test-node.p12");

        final TestableConsole inputCa = TestableConsole.empty()
                .register(CertutilCa.PROMPT_ENTER_CA_PASSWORD, "password");
        final CertutilCa certutilCa = new CertutilCa(caPath.toAbsolutePath().toString(), inputCa);
        certutilCa.run();

        // now we have a ROOT + CA keypair in the keystore, let's use it to generate node keypair

        TestableConsole inputHttp = TestableConsole.empty()
                .register(CertutilHttp.PROMPT_USE_OWN_CERTIFICATE_AUTHORITY, "n")
                .register(CertutilHttp.PROMPT_ENTER_CA_PASSWORD, "password")
                .register(CertutilHttp.PROMPT_ENTER_CERTIFICATE_VALIDITY_IN_DAYS, "90")
                .register(CertutilHttp.PROMPT_ENTER_CERTIFICATE_ALTERNATIVE_NAMES, "example.com")
                .register(CertutilHttp.PROMPT_ENTER_HTTP_CERTIFICATE_PASSWORD, "changeme");

        CertutilHttp certutilCert = new CertutilHttp(
                caPath.toAbsolutePath().toString(),
                nodePath.toAbsolutePath().toString(),
                inputHttp);
        certutilCert.run();

        KeyStore caKeyStore = KeyStore.getInstance("PKCS12");
        caKeyStore.load(new FileInputStream(caPath.toFile()), "password".toCharArray());

        KeyStore nodeKeyStore = KeyStore.getInstance("PKCS12");
        nodeKeyStore.load(new FileInputStream(nodePath.toFile()), "changeme".toCharArray());
        final Key nodeKey = nodeKeyStore.getKey(DATANODE_KEY_ALIAS, "changeme".toCharArray());
        Assertions.assertThat(nodeKey).isNotNull();

        final Certificate nodeCertificate = nodeKeyStore.getCertificate(DATANODE_KEY_ALIAS);
        Assertions.assertThatCode(() -> nodeCertificate.verify(caKeyStore.getCertificate(CertConstants.CA_KEY_ALIAS).getPublicKey()))
                .doesNotThrowAnyException();

        final Collection<List<?>> alternativeNames = ((X509Certificate) nodeCertificate).getSubjectAlternativeNames();
        Assertions.assertThat(alternativeNames)
                .isNotEmpty()
                .extracting(item -> (String) item.get(1))
                .contains("localhost", "example.com");

        var hostname = Tools.getLocalCanonicalHostname();
        final Certificate[] certificateChain = nodeKeyStore.getCertificateChain(DATANODE_KEY_ALIAS);
        Assertions.assertThat(certificateChain)
                .hasSize(2)
                .extracting(c -> (X509Certificate) c)
                .extracting(c -> c.getSubjectX500Principal().getName())
                .contains("CN=Graylog CA", "CN=" + hostname);
    }
}
