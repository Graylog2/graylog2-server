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
package org.graylog.security.certutil.csr;

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
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;
import org.graylog.security.certutil.privatekey.PrivateKeyEncryptedStorage;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPairGenerator;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class CsrGenerator {

    /**
     * Generates new CSR.
     *
     * @param privateKeyPassword         Password to protect private key.
     * @param altNames                   List of alternative names to be stored in CSR
     * @param privateKeyEncryptedStorage Mechanism for storing private keys.
     * @return A new CSR, instance of {@link PKCS10CertificationRequest}
     */
    public PKCS10CertificationRequest generateCSR(final char[] privateKeyPassword,
                                                  final String principalName,
                                                  final List<String> altNames,
                                                  final PrivateKeyEncryptedStorage privateKeyEncryptedStorage) throws CSRGenerationException {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
            java.security.KeyPair certKeyPair = keyGen.generateKeyPair();

            privateKeyEncryptedStorage.writeEncryptedKey(privateKeyPassword, certKeyPair.getPrivate());


            PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=" + principalName),
                    certKeyPair.getPublic()
            );

            if (altNames != null && !altNames.isEmpty()) {
                Extension subjectAltNames = new Extension(Extension.subjectAlternativeName, false,
                        new DEROctetString(
                                new GeneralNames(
                                        altNames.stream()
                                                .map(alternativeName -> new GeneralName(
                                                        new X500Name("CN=" + alternativeName))
                                                )
                                                .toArray(GeneralName[]::new)
                                )
                        )
                );
                p10Builder.addAttribute(
                        PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                        new Extensions(subjectAltNames));
            }


            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SIGNING_ALGORITHM);
            ContentSigner signer = csBuilder.build(certKeyPair.getPrivate());
            return p10Builder.build(signer);

        } catch (Exception e) {
            throw new CSRGenerationException("Failed to generate Certificate Signing Request", e);
        }
    }
}
