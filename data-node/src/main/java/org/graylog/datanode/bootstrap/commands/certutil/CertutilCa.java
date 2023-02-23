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
package org.graylog.datanode.bootstrap.commands.certutil;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import org.graylog2.bootstrap.CliCommand;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Command(name = "ca", description = "Manage certificate authority for data-node", groupNames = {"certutil"})
public class CertutilCa implements CliCommand {

    @Option(name = "--filename", description = "Filename for the CA keystore")
    protected String keystoreFilename = "datanode-ca.p12";
    private final CommandLineConsole console;

    public CertutilCa() {
        this.console = new SystemConsole();
    }

    public CertutilCa(String keystoreFilename, CommandLineConsole console) {
        this.keystoreFilename = keystoreFilename;
        this.console = console;
    }

    @Override
    public void run() {
        try {

            console.printLine("This tool will generate a self-signed certificate authority for datanode");

            char[] password = this.console.readPassword("Enter CA password: ");

            console.printLine("Generating datanode CA");

            final Duration certificateValidity = Duration.ofDays(10 * 365);
            KeyPair rootCA = CertificateGenerator.generate(CertRequest.selfSigned("root").isCA(true).validity(certificateValidity));
            KeyPair intermediateCA = CertificateGenerator.generate(CertRequest.signed("ca", rootCA).isCA(true).validity(certificateValidity));

            KeyStore caKeystore = KeyStore.getInstance("PKCS12");
            caKeystore.load(null, null);

            caKeystore.setKeyEntry("root", rootCA.privateKey(), password,
                    new X509Certificate[]{rootCA.certificate()});
            caKeystore.setKeyEntry("ca", intermediateCA.privateKey(), password,
                    new X509Certificate[]{intermediateCA.certificate(), rootCA.certificate()});

            console.printLine("Private keys and certificates for root and intermediate CA generated");

            final Path keystorePath = Path.of(keystoreFilename);

            try (FileOutputStream store = new FileOutputStream(keystorePath.toFile())) {
                caKeystore.store(store, password);
                console.printLine("Keys and certificates stored in " + keystorePath.toAbsolutePath());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CA certificate", e);
        }
    }
}
