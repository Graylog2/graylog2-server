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
import org.graylog.security.certutil.ca.CACreator;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog.security.certutil.keystore.storage.KeystoreFileStorage;
import org.graylog2.bootstrap.CliCommand;

import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;

@Command(name = "ca", description = "Manage certificate authority for data-node", groupNames = {"certutil"})
public class CertutilCa implements CliCommand {

    @Option(name = "--filename", description = "Filename for the CA keystore")
    protected String keystoreFilename = "datanode-ca.p12";
    private final CommandLineConsole console;
    private final CACreator caCreator;
    private final KeystoreFileStorage caKeystoreStorage;

    public CertutilCa() {
        this.console = new SystemConsole();
        this.caCreator = new CACreator();
        this.caKeystoreStorage = new KeystoreFileStorage();
    }

    public CertutilCa(String keystoreFilename, CommandLineConsole console) {
        this.keystoreFilename = keystoreFilename;
        this.console = console;
        this.caCreator = new CACreator();
        this.caKeystoreStorage = new KeystoreFileStorage();
    }

    @Override
    public void run() {
        try {

            console.printLine("This tool will generate a self-signed certificate authority for datanode");
            char[] password = this.console.readPassword("Enter CA password: ");
            console.printLine("Generating datanode CA");

            final Duration certificateValidity = Duration.ofDays(10 * 365);
            KeyStore caKeystore = caCreator.createCA(password, certificateValidity);

            console.printLine("Private keys and certificates for root and intermediate CA generated");

            final Path keystorePath = Path.of(keystoreFilename);
            //TODO: it is probably a bad idea to use the same password for CA and its storage...
            caKeystoreStorage.writeKeyStore(keystorePath, caKeystore, password);
            console.printLine("Keys and certificates stored in " + keystorePath.toAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CA certificate", e);
        }
    }
}
