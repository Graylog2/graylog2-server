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
package org.graylog.datanode.bootstrap.commands.certutil;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class CertTools {
    /**
     * @param cnName The CN={name} of the certificate. When the certificate is for a domain it should be the domain name
     * @param SAN entries (=subject alternative names)
     * @param issuer Issuer who signs this certificate. Null for a self-signed certificate
     * @param isCA   Can this certificate be used to sign other certificates
     * @return Newly created certificate with its private key
     */
    protected static KeyPair generateCertificate(String cnName, @Nullable List<String> SAN, @Nullable KeyPair issuer, boolean isCA) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        java.security.KeyPair certKeyPair = keyGen.generateKeyPair();
        X500Name name = new X500Name("CN=" + cnName);

        // TODO: cert serial number?
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Instant validFrom = Instant.now();

        // TODO: configure validity!
        Instant validUntil = validFrom.plus(10 * 365, ChronoUnit.DAYS);

        // If there is no issuer, we self-sign our certificate.
        X500Name issuerName;
        PrivateKey issuerKey;
        if (issuer == null) {
            issuerName = name;
            issuerKey = certKeyPair.getPrivate();
        } else {
            issuerName = new X500Name(issuer.certificate().getIssuerX500Principal().getName());
            issuerKey = issuer.privateKey();
        }

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                Date.from(validFrom), Date.from(validUntil),
                name, certKeyPair.getPublic());

        // Make the certificate to a Cert Authority to sign more certs when needed
        if (isCA) {
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCA));
        }

        if (SAN != null) {
            GeneralName[] generalNames = SAN.stream()
                    .map(s -> new GeneralName(GeneralName.dNSName, s))
                    .toArray(GeneralName[]::new);
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(generalNames));
        }

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(issuerKey);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
        return new KeyPair(certKeyPair.getPrivate(), certKeyPair.getPublic(), cert);
    }
}
