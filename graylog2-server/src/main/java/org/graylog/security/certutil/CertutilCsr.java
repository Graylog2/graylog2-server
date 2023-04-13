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
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.security.certutil.console.CommandLineConsole;
import org.graylog.security.certutil.console.SystemConsole;
import org.graylog.security.certutil.csr.CsrFileStorage;
import org.graylog.security.certutil.csr.CsrStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedFileStorage;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;
import org.graylog2.bootstrap.CliCommand;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPairGenerator;


@Command(name = "csr", description = "Create CSR", groupNames = {"certutil"})
public class CertutilCsr implements CliCommand {

    @Option(name = "--privateKey", description = "Keystore with private key")
    protected String privateKeyFilename = "csr-private-key.key";

    @Option(name = "--csrFile", description = "Keystore with private key")
    protected String csrFilename = "csr.csr";

    private final CommandLineConsole console;
    private final PrivateKeyEncryptedStorage privateKeyEncryptedFileStorage;
    private final CsrStorage csrFileStorage;

    public CertutilCsr() {
        this.console = new SystemConsole();
        this.privateKeyEncryptedFileStorage = new PrivateKeyEncryptedFileStorage(privateKeyFilename);
        this.csrFileStorage = new CsrFileStorage(csrFilename);
    }

    public CertutilCsr(final String privateKeyFilename,
                       final String csrFilename,
                       final CommandLineConsole console) {
        this.privateKeyFilename = privateKeyFilename;
        this.csrFilename = csrFilename;
        this.console = console;
        this.privateKeyEncryptedFileStorage = new PrivateKeyEncryptedFileStorage(privateKeyFilename);
        this.csrFileStorage = new CsrFileStorage(csrFilename);
    }

    @Override
    public void run() {
        console.printLine("This tool will generate a CSR for the datanode");
        char[] privKeyPassword = this.console.readPassword("Enter password to protect your private key : ");

        try {
            console.printLine("Generating CSR for the datanode");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            java.security.KeyPair certKeyPair = keyGen.generateKeyPair();

            privateKeyEncryptedFileStorage.writeEncryptedKey(privKeyPassword, certKeyPair.getPrivate());

            Extension subjectAltName = new Extension(Extension.subjectAlternativeName, false,
                    new DEROctetString(new GeneralNames(new GeneralName(new X500Name("CN=Alt Name")))));

            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=localhost"), certKeyPair.getPublic())
                    .addAttribute(
                            PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                            new Extensions(subjectAltName));
            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder("SHA256withRSA");
            ContentSigner signer = csBuilder.build(certKeyPair.getPrivate());
            PKCS10CertificationRequest csr = p10Builder.build(signer);

            csrFileStorage.writeCsr(csr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
