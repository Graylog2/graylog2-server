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
package org.graylog.security.certutil;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog2.bootstrap.CliCommand;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Locale;

import static org.graylog.security.certutil.CertConstants.CA_KEY_ALIAS;
import static org.graylog.security.certutil.CertConstants.PKCS12;


@Command(name = "truststore", description = "Manage certificates for data-node", groupNames = {"certutil"})
public class CertutilTruststore implements CliCommand {

    @Option(name = "--ca", description = "Filename for the CA keystore")
    protected String caKeystoreFilename = "datanode-ca.p12";

    @Option(name = "--truststore", description = "Filename for the generated truststore")
    protected String truststoreFilename = "datanode-truststore.p12";

    private final CommandLineConsole console;

    public static final CommandLineConsole.Prompt PROMPT_ENTER_CA_PASSWORD = CommandLineConsole.prompt("Enter CA password: ");
    public static final CommandLineConsole.Prompt PROMPT_ENTER_TRUSTSTORE_PASSWORD = CommandLineConsole.prompt("Enter datanode truststore password: ");

    public CertutilTruststore() {
        this.console = new SystemConsole();
    }

    public CertutilTruststore(String caKeystoreFilename, String truststoreFilename, CommandLineConsole console) {
        this.caKeystoreFilename = caKeystoreFilename;
        this.truststoreFilename = truststoreFilename;
        this.console = console;
    }

    @Override
    public void run() {
        console.printLine("This tool will generate a truststore with certificate of provided certificate authority");

        final Path caKeystorePath = Path.of(caKeystoreFilename);

        console.printLine("Using certificate authority " + caKeystorePath.toAbsolutePath());

        try {

            if(!Files.exists(caKeystorePath)) {
                throw new IllegalArgumentException("File " + caKeystorePath.toAbsolutePath() + " doesn't exist!");
            }

            char[] password = console.readPassword(PROMPT_ENTER_CA_PASSWORD);
            KeyStore caKeystore = KeyStore.getInstance(PKCS12);
            caKeystore.load(new FileInputStream(caKeystorePath.toFile()), password);

            final X509Certificate caCertificate = (X509Certificate) caKeystore.getCertificate(CA_KEY_ALIAS);

            console.printLine("Successfully read CA from the keystore");
            console.printLine(certificateInfo(caCertificate));

            KeyStore truststore = KeyStore.getInstance(PKCS12);
            truststore.load(null, null);

            char[] truststorePassword = console.readPassword(PROMPT_ENTER_TRUSTSTORE_PASSWORD);

            truststore.setCertificateEntry(CA_KEY_ALIAS, caCertificate);

            final Path nodeKeystorePath = Path.of(truststoreFilename);
            try (FileOutputStream store = new FileOutputStream(nodeKeystorePath.toFile())) {
                truststore.store(store, truststorePassword);
                console.printLine("Truststore with the CA certificate successfully saved into " + nodeKeystorePath.toAbsolutePath());
            }

            // TODO: provide good user-friendly error message for each exception type!
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static String certificateInfo(X509Certificate cert) {
        return String.format(Locale.ROOT, "Subject: %s, issuer: %s, not before: %s, not after: %s", cert.getSubjectX500Principal(), cert.getIssuerX500Principal(), cert.getNotBefore(), cert.getNotAfter());
    }
}
