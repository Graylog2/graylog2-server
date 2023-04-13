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
import org.graylog.security.certutil.CertutilCa;
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

class CertutilCaTest {

    @TempDir
    static Path tempDir;

    @Test
    void testCaCertificateGeneration() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidAlgorithmParameterException, CertPathValidatorException, SignatureException, InvalidKeyException, NoSuchProviderException {

        final Path certPath = tempDir.resolve("test-ca.p12");

        final TestableConsole input = TestableConsole.empty()
                .register("Enter CA password", "password");

        final CertutilCa command = new CertutilCa(certPath.toAbsolutePath().toString(), input);
        command.run();

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new FileInputStream(certPath.toFile()), "password".toCharArray());

        Assertions.assertThat(keyStore.getKey("root", "password".toCharArray())).isNotNull();
        Assertions.assertThat(keyStore.getKey("ca", "password".toCharArray())).isNotNull();
        final Certificate rootCert = keyStore.getCertificate("root");
        Assertions.assertThat(rootCert).isNotNull();

        final Certificate caCert = keyStore.getCertificate("ca");

        Assertions.assertThatCode(() -> caCert.verify(rootCert.getPublicKey()))
                .doesNotThrowAnyException();


        /*
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        final ArrayList<Certificate> objects = new ArrayList<>();
        objects.add(keyStore.getCertificate("root"));
        objects.add(keyStore.getCertificate("ca"));
        CertPath path = cf.generateCertPath(objects);

        PKIXParameters params = new PKIXParameters(keyStore);

        validator.validate(path, params);

         */
    }
}
