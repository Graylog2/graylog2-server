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

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class CsrSigner {
    private static boolean isValidName(final int name) {
        return switch (name) {
            case GeneralName.dNSName, GeneralName.iPAddress, GeneralName.rfc822Name -> true;
            default -> false;
        };
    }

    public static X509Certificate sign(PrivateKey caPrivateKey, X509Certificate caCertificate, PKCS10CertificationRequest csr, int validityDays) throws Exception {
        // TODO: cert serial number?
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Instant validFrom = Instant.now();

        Instant validUntil = validFrom.plus(Duration.ofDays(validityDays));

        var issuerName = X500Name.getInstance(caCertificate.getSubjectX500Principal().getEncoded());
        var issuerKey = caPrivateKey;

        var builder = new X509v3CertificateBuilder(
                issuerName,
                serialNumber,
                Date.from(validFrom), Date.from(validUntil),
                csr.getSubject(), csr.getSubjectPublicKeyInfo());

        ArrayList<GeneralName> altNames = new ArrayList<>();
        Optional.ofNullable(csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)).ifPresent(certAttributes ->
            Arrays.stream(certAttributes).forEach(attribute -> {
                Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
                GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                Optional.ofNullable(gns).ifPresent(g -> Arrays.stream(g.getNames()).filter(name -> isValidName(name.getTagNo())).forEach(altNames::add));
            }));
        if (!altNames.isEmpty()) {
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(altNames.toArray(new GeneralName[altNames.size()])));
        }

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(issuerKey);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
        return cert;
    }

}
