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
import org.graylog2.plugin.Tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;

@Command(name = "cert", description = "Manage certificates for data-node", groupNames = {"certutil"})
public class CertutilCert implements CliCommand {

    public static final String DATANODE_KEY_ALIAS = "datanode";
    @Option(name = "--ca", description = "Filename for the CA keystore")
    protected String caKeystoreFilename = "datanode-ca.p12";

    @Option(name = "--keystore", description = "Filename for the generated keystore")
    protected String nodeKeystoreFilename = "datanode-transport-certificates.p12";

    private final CommandLineConsole console;

    public CertutilCert() {
        this.console = new SystemConsole();
    }

    public CertutilCert(String caKeystoreFilename, String nodeKeystoreFilename, CommandLineConsole console) {
        this.caKeystoreFilename = caKeystoreFilename;
        this.nodeKeystoreFilename = nodeKeystoreFilename;
        this.console = console;
    }

    @Override
    public void run() {
        console.printLine("This tool will generate a data-node certificate signed by provided certificate authority");

        final Path caKeystorePath = Path.of(caKeystoreFilename);

        console.printLine("Using certificate authority " + caKeystorePath.toAbsolutePath());

        try {
            char[] password = console.readPassword("Enter CA password: ");
            KeyStore caKeystore = KeyStore.getInstance("PKCS12");
            caKeystore.load(new FileInputStream(caKeystorePath.toFile()), password);

            final Key caPrivateKey = caKeystore.getKey("ca", password);

            final X509Certificate caCertificate = (X509Certificate) caKeystore.getCertificate("ca");
            final X509Certificate rootCertificate = (X509Certificate) caKeystore.getCertificate("root");

            console.printLine("Successfully read CA from the keystore");

            final KeyPair intermediateCA = new KeyPair((PrivateKey) caPrivateKey, null, caCertificate);

            console.printLine("Generating private key and certificate for this datanode");

            final CertRequest req = CertRequest.signed("localhost", intermediateCA)
                    .withSubjectAlternativeName("localhost")
                    .withSubjectAlternativeName(Tools.getLocalHostname())
                    .withSubjectAlternativeName(String.valueOf(InetAddress.getLocalHost()))
                    .withSubjectAlternativeName("127.0.0.1")
                    .withSubjectAlternativeName("ip6-localhost")
                    .validity(Duration.ofDays(10 * 365));
            KeyPair nodePair = CertificateGenerator.generate(req);

            KeyStore nodeKeystore = KeyStore.getInstance("PKCS12");
            nodeKeystore.load(null, null);

            char[] nodeKeystorePassword = console.readPassword("Enter datanode certificate password: ");

            nodeKeystore.setKeyEntry(DATANODE_KEY_ALIAS, nodePair.privateKey(), nodeKeystorePassword,
                    new X509Certificate[]{nodePair.certificate(), intermediateCA.certificate(), rootCertificate});


            final Path nodeKeystorePath = Path.of(nodeKeystoreFilename);
            try (FileOutputStream store = new FileOutputStream(nodeKeystorePath.toFile())) {
                nodeKeystore.store(store, nodeKeystorePassword);
                console.printLine("Private key and certificate for this datanode successfully saved into " + nodeKeystorePath.toAbsolutePath());
            }

        // TODO: provide good user-friendly error message for each exception type!
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException |
                 UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
