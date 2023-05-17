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
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.SuppressForbidden;

import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;

import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

@Command(name = "http", description = "Manage certificates for data-node", groupNames = {"certutil"})
public class CertutilHttp implements CliCommand {

    public static final String DATANODE_KEY_ALIAS = "datanode";
    @Option(name = "--ca", description = "Filename for the CA keystore")
    protected String caKeystoreFilename = "datanode-ca.p12";

    @Option(name = "--keystore", description = "Filename for the generated HTTP keystore")
    protected String nodeKeystoreFilename = "datanode-http-certificates.p12";

    private final CommandLineConsole console;

    public CertutilHttp() {
        this.console = new SystemConsole();
    }

    public CertutilHttp(String caKeystoreFilename, String nodeKeystoreFilename, CommandLineConsole console) {
        this.caKeystoreFilename = caKeystoreFilename;
        this.nodeKeystoreFilename = nodeKeystoreFilename;
        this.console = console;
    }

    @Override
    @SuppressForbidden("DNS Lookup intentional.")
    public void run() {
        console.printLine("This tool will generate a data-node certificate for HTTP communication (REST API)");

        final boolean useOwnCertificateEntity = console.readBoolean("Do you want to use your own certificate authority? Respond with y/n?");

        if (useOwnCertificateEntity) {
            try {
                console.printLine("Generating certificate signing request for this datanode");
                final CertRequest certReq = CertRequest.selfSigned("localhost");
                KeyPair keyPair = CertificateGenerator.generate(certReq);
                PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                        new X500Principal("CN=Requested Test Certificate"), keyPair.publicKey());
                JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SIGNING_ALGORITHM);
                ContentSigner signer = csBuilder.build(keyPair.privateKey());
                PKCS10CertificationRequest csr = p10Builder.build(signer);

                final Path csrPath = Path.of("datanode.csr");
                writePem(csrPath, csr);

                console.printLine("CSR written to file " + csrPath.toAbsolutePath());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
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
                final X509Certificate rootCertificate = (X509Certificate) caKeystore.getCertificate("root");

                final KeyPair caKeyPair = new KeyPair(caPrivateKey, null, caCertificate);

                final int validityDays = console.readInt("Enter certificate validity in days: ");

                final String cnName = "localhost";

                CertRequest certificateRequest = CertRequest.signed(cnName, caKeyPair)
                        .withSubjectAlternativeName(cnName)
                        .withSubjectAlternativeName(Tools.getLocalHostname())
                        .withSubjectAlternativeName(String.valueOf(InetAddress.getLocalHost()))
                        .withSubjectAlternativeName("127.0.0.1")
                        .withSubjectAlternativeName("ip6-localhost")
                        .validity(Duration.ofDays(validityDays));

                final String alternativeNames = console.readLine("Enter alternative names (addresses) of this node [comma separated]: ");
                Arrays.stream(alternativeNames.split(","))
                        .filter(Strings::isNotBlank)
                        .forEach(certificateRequest::withSubjectAlternativeName);

                console.printLine(String.format(Locale.ROOT, "Generating certificate for CN=%s, with validity %d days and subject alternative names %s", cnName, certificateRequest.validity().toDays(), certificateRequest.subjectAlternativeNames()));
                KeyPair nodePair = CertificateGenerator.generate(certificateRequest);
                console.printLine("Successfully generated CA from the keystore");

                KeyStore nodeKeystore = KeyStore.getInstance("PKCS12");
                nodeKeystore.load(null, null);

                char[] nodeKeystorePassword = console.readPassword("Enter HTTP certificate password: ");

                nodeKeystore.setKeyEntry(DATANODE_KEY_ALIAS, nodePair.privateKey(), nodeKeystorePassword,
                        new X509Certificate[]{nodePair.certificate(), caKeyPair.certificate(), rootCertificate});


                final Path nodeKeystorePath = Path.of(nodeKeystoreFilename);
                try (FileOutputStream store = new FileOutputStream(nodeKeystorePath.toFile())) {
                    nodeKeystore.store(store, nodeKeystorePassword);
                    console.printLine("Private key and certificate for this datanode HTTP successfully saved into " + nodeKeystorePath.toAbsolutePath());
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

    private static void writePem(Path path, Object object) throws IOException {
        FileWriter writer = new FileWriter(path.toFile(), StandardCharsets.UTF_8);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(object);
        pemWriter.flush();
        pemWriter.close();
    }
}
