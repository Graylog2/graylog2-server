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
import org.graylog.security.certutil.CertutilTruststore;
import org.graylog.security.certutil.console.TestableConsole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
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

class CertutilTruststoreTest {

    @TempDir
    static Path tempDir;

    @Test
    void testGenerateTruststore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidAlgorithmParameterException, CertPathValidatorException, SignatureException, InvalidKeyException, NoSuchProviderException {

        final Path caPath = tempDir.resolve("test-ca.p12");
        final Path truststorePath = tempDir.resolve("truststore.p12");

        final TestableConsole inputCa = TestableConsole.empty()
                .silent()
                .register(CertutilCa.PROMPT_ENTER_CA_PASSWORD, "asdfgh");
        final CertutilCa certutilCa = new CertutilCa(caPath.toAbsolutePath().toString(), inputCa);
        certutilCa.run();

        // now we have a ROOT + CA keypair in the keystore, let's use it to generate the keystore

        TestableConsole inputHttp = TestableConsole.empty()
                .silent()
                .register(CertutilTruststore.PROMPT_ENTER_CA_PASSWORD, "asdfgh")
                .register(CertutilTruststore.PROMPT_ENTER_TRUSTSTORE_PASSWORD, "asdfgh");

        CertutilTruststore certutilCert = new CertutilTruststore(
                caPath.toAbsolutePath().toString(),
                truststorePath.toAbsolutePath().toString(),
                inputHttp);
        certutilCert.run();

        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(new FileInputStream(truststorePath.toFile()), "asdfgh".toCharArray());

        final Certificate cert = truststore.getCertificate(CertConstants.CA_KEY_ALIAS);
        Assertions.assertThat(cert)
                .extracting(c -> (X509Certificate) c)
                .extracting(c -> c.getSubjectX500Principal().getName())
                .isEqualTo("CN=Graylog CA");
    }

    @Test
    void testUnknownCaPath(@TempDir Path tempDir) {
        TestableConsole inputHttp = TestableConsole.empty()
                .silent()
                .register(CertutilTruststore.PROMPT_ENTER_CA_PASSWORD, "asdfgh")
                .register(CertutilTruststore.PROMPT_ENTER_TRUSTSTORE_PASSWORD, "asdfgh");

        final String caPath = tempDir.resolve("unknown-file.p12").toAbsolutePath().toString();

        CertutilTruststore certutilCert = new CertutilTruststore(
                caPath,
                tempDir.resolve("truststore.p12").toAbsolutePath().toString(),
                inputHttp);

        Assertions.assertThatThrownBy(certutilCert::run)
                        .hasMessageContaining("File " + caPath + " doesn't exist!");
    }
}
