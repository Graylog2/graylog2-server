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

import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
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
import org.graylog2.plugin.certificates.RenewalPolicy;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.bouncycastle.asn1.x509.GeneralName.dNSName;
import static org.bouncycastle.asn1.x509.GeneralName.iPAddress;
import static org.bouncycastle.asn1.x509.GeneralName.rfc822Name;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class CsrSigner {
    private final Clock clock;

    public CsrSigner() {
        this.clock = Clock.systemDefaultZone();
    }

    public CsrSigner(Clock clock) {
        this.clock = clock;
    }

    private boolean isValidName(final int name) {
        return switch (name) {
            case dNSName, iPAddress, rfc822Name -> true;
            default -> false;
        };
    }

    private int periodToDays(Period period) {
        return period.getYears() * 365 + period.getMonths() * 30 + period.getDays();
    }

    public X509Certificate sign(PrivateKey caPrivateKey, X509Certificate caCertificate, PKCS10CertificationRequest csr, RenewalPolicy renewalPolicy) throws Exception {
        Instant validFrom = Instant.now(clock);
        final var lifetime = Period.parse(renewalPolicy.certificateLifetime());
        var validUntil = validFrom.plus(periodToDays(lifetime), ChronoUnit.DAYS);

        return sign(caPrivateKey, caCertificate, csr, validFrom, validUntil);
    }

    public X509Certificate sign(PrivateKey caPrivateKey, X509Certificate caCertificate, PKCS10CertificationRequest csr, int validityDays) throws Exception {
        Instant validFrom = Instant.now(clock);
        Instant validUntil = validFrom.plus(Duration.ofDays(validityDays));

        return sign(caPrivateKey, caCertificate, csr, validFrom, validUntil);
    }

    private X509Certificate sign(PrivateKey caPrivateKey, X509Certificate caCertificate, PKCS10CertificationRequest csr, Instant validFrom, Instant validUntil) throws Exception {
        // TODO: cert serial number?
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

        var issuerName = X500Name.getInstance(caCertificate.getSubjectX500Principal().getEncoded());
        var issuerKey = caPrivateKey;

        var builder = new X509v3CertificateBuilder(
                issuerName,
                serialNumber,
                Date.from(validFrom), Date.from(validUntil),
                csr.getSubject(), csr.getSubjectPublicKeyInfo());

        var certAttributes = csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest);
        if (certAttributes != null && certAttributes.length > 0) {
            ArrayList<GeneralName> altNames = new ArrayList<>();

            for (Attribute attribute : certAttributes) {
                Extensions extensions = Extensions.getInstance(attribute.getAttrValues().getObjectAt(0));
                GeneralNames gns = GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName);
                if (gns != null && gns.getNames() != null) {
                    Arrays.stream(gns.getNames()).filter(n -> isValidName(n.getTagNo())).forEach(altNames::add);
                }
            }
            if (!altNames.isEmpty()) {
                builder.addExtension(Extension.subjectAlternativeName, false,
                        new GeneralNames(altNames.toArray(new GeneralName[altNames.size()])));
            }
        }

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(issuerKey);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certHolder);
        return cert;
    }

}
