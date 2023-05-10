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
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog.security.certutil.csr.CsrSigner;
import org.graylog.security.certutil.csr.storage.CsrFileStorage;
import org.graylog.security.certutil.csr.storage.CsrStorage;
import org.graylog2.bootstrap.CliCommand;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Command(name = "csrsign", description = "Signs a CSR using the given CA", groupNames = {"certutil"})
public class CertutilCsrSign implements CliCommand {
    @Option(name = "--ca", description = "Filename for the CA keystore")
    protected String caKeystoreFilename = "datanode-ca.p12";

    @Option(name = "--csrFile", description = "The CSR to sign")
    protected String csrFilename = "csr.csr";

    @Option(name = "--certFile", description = "Filename for the signed certificate")
    protected String certFilename = "datanode-cert.p12";

    private final CommandLineConsole console;

    private final CsrStorage csrStorage;

    public CertutilCsrSign() {
        this.console = new SystemConsole();
        this.csrStorage = new CsrFileStorage(csrFilename);
    }

    public CertutilCsrSign(String caKeystoreFilename, String csrFilename, String certFilename, CommandLineConsole console) {
        this.caKeystoreFilename = caKeystoreFilename;
        this.certFilename = certFilename;
        this.csrStorage = new CsrFileStorage(csrFilename);
        this.console = console;
    }

    @Override
    public void run() {
        console.printLine("This tool will generate a data-node certificate for HTTP communication (REST API)");

        // we'll use our own generated CA
            console.printLine("Generating a HTTP certificate signed by the datanode CA");

            final Path caKeystorePath = Path.of(caKeystoreFilename);
            console.printLine("Using certificate authority " + caKeystorePath.toAbsolutePath());

            try {
                char[] password = console.readPassword("Enter CA password: ");
                KeyStore caKeystore = KeyStore.getInstance("PKCS12");
                caKeystore.load(new FileInputStream(caKeystoreFilename), password);

                final PrivateKey caPrivateKey = (PrivateKey) caKeystore.getKey("ca", password);
                final X509Certificate caCertificate = (X509Certificate) caKeystore.getCertificate("ca");
                final var csr = csrStorage.readCsr();
                final int validityDays = console.readInt("Enter certificate validity in days: ");

                final var cert = CsrSigner.sign(caPrivateKey, caCertificate, csr, validityDays);

                final Path certPath = Path.of(certFilename);
                writePem(certPath, cert);

                console.printLine("Certificate written to file " + certPath.toAbsolutePath());

                // TODO: provide good user-friendly error message for each exception type!
            } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException |
                     UnrecoverableKeyException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

    }

    private static void writePem(Path path, Object object) throws IOException {
        FileWriter writer = new FileWriter(path.toFile(), StandardCharsets.UTF_8);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(object);
        pemWriter.flush();
        pemWriter.close();
    }
}
