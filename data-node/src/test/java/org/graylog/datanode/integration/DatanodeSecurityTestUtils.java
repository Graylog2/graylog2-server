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
import org.graylog.security.certutil.CertutilCa;
import org.graylog.security.certutil.CertutilCert;
import org.graylog.security.certutil.CertutilHttp;
import org.graylog.security.certutil.console.TestableConsole;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.csr.KeystoreInformation;
import org.graylog2.security.TruststoreCreator;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;

public class DatanodeSecurityTestUtils {
    public static KeyStore buildTruststore(KeystoreInformation ca) throws IOException, GeneralSecurityException {
        final TruststoreCreator truststoreCreator = TruststoreCreator.newEmpty();
        final Enumeration<String> aliases = ca.loadKeystore().aliases();
        while (aliases.hasMoreElements()) {
            final String alias = aliases.nextElement();
            truststoreCreator.addFromKeystore(ca, alias);
        }
        return truststoreCreator.getTruststore();
    }

    public static FilesystemKeystoreInformation generateCa(Path dir) {
        final Path certPath = dir.resolve("test-ca.p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        final TestableConsole input = TestableConsole.empty().silent()
                .register(CertutilCa.PROMPT_ENTER_CA_PASSWORD, password);
        final CertutilCa command = new CertutilCa(certPath.toAbsolutePath().toString(), input);
        command.run();
        return new FilesystemKeystoreInformation(certPath, password.toCharArray());
    }

    public static FilesystemKeystoreInformation generateTransportCert(Path dir, FilesystemKeystoreInformation ca, String... containerHostnames) {
        final Path transportPath = dir.resolve("transport-" + RandomStringUtils.randomAlphabetic(10) + ".p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        TestableConsole inputCert = TestableConsole.empty().silent()
                .register(CertutilCert.PROMPT_ENTER_CA_PASSWORD, new String(ca.password()))
                .register(CertutilCert.PROMPT_ENTER_CERTIFICATE_PASSWORD, password)
                .register(CertutilCert.PROMPT_ENTER_CERT_ALTERNATIVE_NAMES, String.join(",", containerHostnames));
        CertutilCert certutilCert = new CertutilCert(
                ca.location().toAbsolutePath().toString(),
                transportPath.toAbsolutePath().toString(),
                inputCert);
        certutilCert.run();
        return new FilesystemKeystoreInformation(transportPath, password.toCharArray());
    }

    public static FilesystemKeystoreInformation generateHttpCert(Path dir, FilesystemKeystoreInformation ca, String... containerHostnames) {
        final Path httpPath = dir.resolve("http-" + RandomStringUtils.randomAlphabetic(10) + ".p12");
        final String password = RandomStringUtils.randomAlphabetic(10);
        TestableConsole inputHttp = TestableConsole.empty().silent()
                .register(CertutilHttp.PROMPT_USE_OWN_CERTIFICATE_AUTHORITY, "n")
                .register(CertutilHttp.PROMPT_ENTER_CA_PASSWORD, new String(ca.password()))
                .register(CertutilHttp.PROMPT_ENTER_CERTIFICATE_VALIDITY_IN_DAYS, "90")
                .register(CertutilHttp.PROMPT_ENTER_CERTIFICATE_ALTERNATIVE_NAMES, String.join(",", containerHostnames))
                .register(CertutilHttp.PROMPT_ENTER_HTTP_CERTIFICATE_PASSWORD, password);
        CertutilHttp certutilCert = new CertutilHttp(
                ca.location().toAbsolutePath().toString(),
                httpPath.toAbsolutePath().toString(),
                inputHttp);
        certutilCert.run();
        return new FilesystemKeystoreInformation(httpPath, password.toCharArray());
    }
}
