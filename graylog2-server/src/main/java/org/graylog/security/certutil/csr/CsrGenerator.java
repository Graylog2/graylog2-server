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
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.graylog.security.certutil.CertConstants;
import org.graylog.security.certutil.csr.exceptions.CSRGenerationException;

import javax.security.auth.x500.X500Principal;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class CsrGenerator {

    /**
     * Generates new CSR.
     *
     * @param altNames List of alternative names to be stored in CSR
     * @return A new CSR, instance of {@link PKCS10CertificationRequest}
     */
    public static PKCS10CertificationRequest generateCSR(KeystoreInformation keystoreInformation, String alias,
                                                         final String principalName,
                                                         final List<String> altNames) throws CSRGenerationException {
        try {

            final KeyStore keystore = keystoreInformation.loadKeystore();

            final var p10Builder = new JcaPKCS10CertificationRequestBuilder(
                    new X500Principal("CN=" + principalName),
                    keystore.getCertificate(alias).getPublicKey()
            );

            final var names = new ArrayList<>(List.of(principalName));
            if (altNames != null) {
                names.addAll(altNames);
            }

            Extension subjectAltNames = new Extension(Extension.subjectAlternativeName, false,
                    new DEROctetString(
                            new GeneralNames(
                                    names.stream()
                                            .map(alternativeName -> new GeneralName(GeneralName.dNSName, alternativeName))
                                            .toArray(GeneralName[]::new)
                            )
                    )
            );
            p10Builder.addAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                    new Extensions(subjectAltNames));

            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SIGNING_ALGORITHM);
            ContentSigner signer = csBuilder.build((PrivateKey) keystore.getKey(alias, keystoreInformation.password()));
            return p10Builder.build(signer);

        } catch (Exception e) {
            throw new CSRGenerationException("Failed to generate Certificate Signing Request", e);
        }
    }
}
