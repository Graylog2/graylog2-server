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
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog.security.certutil.csr.CsrGenerator;
import org.graylog.security.certutil.csr.storage.CsrFileStorage;
import org.graylog.security.certutil.csr.storage.CsrStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;
import org.graylog2.bootstrap.CliCommand;

import java.util.List;


@Command(name = "csr", description = "Create CSR", groupNames = {"certutil"})
public class CertutilCsr implements CliCommand {

    @Option(name = "--privateKey", description = "Keystore with private key")
    protected String privateKeyFilename = "csr-private-key.key";

    @Option(name = "--csrFile", description = "Keystore with private key")
    protected String csrFilename = "csr.csr";

    private final CommandLineConsole console;
    private final PrivateKeyEncryptedStorage privateKeyEncryptedStorage;
    private final CsrStorage csrStorage;
    private final CsrGenerator csrGenerator;

    public CertutilCsr() {
        this.console = new SystemConsole();
        this.privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage(privateKeyFilename);
        this.csrStorage = new CsrFileStorage(csrFilename);
        this.csrGenerator = new CsrGenerator();
    }

    public CertutilCsr(final String privateKeyFilename,
                       final String csrFilename,
                       final CommandLineConsole console) {
        this.privateKeyFilename = privateKeyFilename;
        this.csrFilename = csrFilename;
        this.console = console;
        this.privateKeyEncryptedStorage = new PrivateKeyEncryptedFileStorage(privateKeyFilename);
        this.csrStorage = new CsrFileStorage(csrFilename);
        this.csrGenerator = new CsrGenerator();
    }

    @Override
    public void run() {
        console.printLine("This tool will generate a CSR for the datanode");
        char[] privKeyPassword = this.console.readPassword("Enter password to protect your private key : ");

        try {
            console.printLine("Generating CSR for the datanode");
            final PKCS10CertificationRequest csr = csrGenerator.generateCSR(
                    privKeyPassword,
                    "localhost",
                    List.of("data-node"),
                    privateKeyEncryptedStorage);
            csrStorage.writeCsr(csr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
