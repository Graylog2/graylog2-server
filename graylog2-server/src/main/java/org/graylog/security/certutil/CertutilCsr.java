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
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.FilesystemKeystoreInformation;
import org.graylog.security.certutil.csr.storage.CsrFileStorage;
import org.graylog.security.certutil.csr.storage.CsrStorage;
import org.graylog2.bootstrap.CliCommand;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;


@Command(name = "csr", description = "Create CSR", groupNames = {"certutil"})
public class CertutilCsr implements CliCommand {

    @Option(name = "--keystore", description = "Keystore with private key", typeConverterProvider = PathTypeConverterProvider.class)
    protected Path keystore = Path.of("keystore.jks");

    @Option(name = "--csrFile", description = "Certificate signing request file")
    protected String csrFilename = "csr.csr";

    private final CommandLineConsole console;
    private final CsrStorage csrStorage;

    public static final CommandLineConsole.Prompt PROMPT_ENTER_PASSWORD_TO_PROTECT_YOUR_PRIVATE_KEY = CommandLineConsole.prompt("Enter password to protect your private key : ");

    public CertutilCsr() {
        this.console = new SystemConsole();
        this.csrStorage = new CsrFileStorage(csrFilename);
    }

    public CertutilCsr(final Path keystore,
                       final String csrFilename,
                       final CommandLineConsole console) {
        this.keystore = keystore;
        this.csrFilename = csrFilename;
        this.console = console;
        this.csrStorage = new CsrFileStorage(csrFilename);
    }

    @Override
    public void run() {
        console.printLine("This tool will generate a CSR for the datanode");
        char[] privKeyPassword = this.console.readPassword(PROMPT_ENTER_PASSWORD_TO_PROTECT_YOUR_PRIVATE_KEY);

        // This will be the only key in the keystore, we don't care much about the alias. To make sure we are
        // not dependent on a specific alias, we can generate a random alphabetic sequence.
        final String alias = RandomStringUtils.secure().nextAlphabetic(10);

        try {
            final KeyPair keyPair = CertificateGenerator.generate(CertRequest.selfSigned(alias).isCA(false).validity(Duration.ofDays(99 * 365)));

            final KeyStore keystore = keyPair.toKeystore(alias, privKeyPassword);
            try (FileOutputStream fos = new FileOutputStream(this.keystore.toFile())) {
                keystore.store(fos, privKeyPassword);
            }

            final FilesystemKeystoreInformation keystoreInformation = new FilesystemKeystoreInformation(this.keystore, privKeyPassword);

            console.printLine("Generating CSR for the datanode");
            final PKCS10CertificationRequest csr = CsrGenerator.generateCSR(
                    keystoreInformation,
                    alias,
                    "localhost",
                    List.of("data-node"));
            csrStorage.writeCsr(csr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
