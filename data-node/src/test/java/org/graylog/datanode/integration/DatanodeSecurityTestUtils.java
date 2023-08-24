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
package org.graylog.datanode.integration;

import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog.security.certutil.console.TestableConsole;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class DatanodeSecurityTestUtils {
    public static KeyStore buildTruststore(KeystoreInformation ca) throws IOException, GeneralSecurityException {
        try (FileInputStream fis = new FileInputStream(ca.location().toFile())) {

            KeyStore caKeystore = KeyStore.getInstance("PKCS12");
            caKeystore.load(fis, ca.password());

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(null, null);

            final Enumeration<String> aliases = caKeystore.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                final Certificate cert = caKeystore.getCertificate(alias);
                if (cert instanceof final X509Certificate x509Certificate) {
                    trustStore.setCertificateEntry(alias, x509Certificate);
                }
            }
            return trustStore;
        }
    }

    public static KeystoreInformation generateCa(Path dir) {
        final Path certPath = dir.resolve("test-ca.p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        final TestableConsole input = TestableConsole.empty().silent()
                .register(CertutilCa.PROMPT_ENTER_CA_PASSWORD, password);
        final CertutilCa command = new CertutilCa(certPath.toAbsolutePath().toString(), input);
        command.run();
        return new KeystoreInformation(certPath, password);
    }

    public static KeystoreInformation generateTransportCert(Path dir, KeystoreInformation ca, String... containerHostnames) {
        final Path transportPath = dir.resolve("transport-" + RandomStringUtils.randomAlphabetic(10) + ".p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        TestableConsole inputCert = TestableConsole.empty().silent()
                .register(CertutilCert.PROMPT_ENTER_CA_PASSWORD, ca.passwordAsString())
                .register(CertutilCert.PROMPT_ENTER_CERTIFICATE_PASSWORD, password)
                .register(CertutilCert.PROMPT_ENTER_CERT_ALTERNATIVE_NAMES, String.join(",", containerHostnames));
        CertutilCert certutilCert = new CertutilCert(
                ca.location().toAbsolutePath().toString(),
                transportPath.toAbsolutePath().toString(),
                inputCert);
        certutilCert.run();
        return new KeystoreInformation(transportPath, password);
    }

    public static KeystoreInformation generateHttpCert(Path dir, KeystoreInformation ca, String... containerHostnames) {
        final Path httpPath = dir.resolve("http-" + RandomStringUtils.randomAlphabetic(10) + ".p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        TestableConsole inputHttp = TestableConsole.empty().silent()
                .register(CertutilHttp.PROMPT_USE_OWN_CERTIFICATE_AUTHORITY, "n")
                .register(CertutilHttp.PROMPT_ENTER_CA_PASSWORD, ca.passwordAsString())
                .register(CertutilHttp.PROMPT_ENTER_CERTIFICATE_VALIDITY_IN_DAYS, "90")
                .register(CertutilHttp.PROMPT_ENTER_CERTIFICATE_ALTERNATIVE_NAMES, String.join(",", containerHostnames))
                .register(CertutilHttp.PROMPT_ENTER_HTTP_CERTIFICATE_PASSWORD, password);
        CertutilHttp certutilCert = new CertutilHttp(
                ca.location().toAbsolutePath().toString(),
                httpPath.toAbsolutePath().toString(),
                inputHttp);
        certutilCert.run();
        return new KeystoreInformation(httpPath, password);
    }
}
