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

import com.google.common.collect.Sets;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.bouncycastle.asn1.x509.GeneralName.dNSName;
import static org.bouncycastle.asn1.x509.GeneralName.iPAddress;
import static org.bouncycastle.asn1.x509.GeneralName.rfc822Name;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;

public class CsrSigner {
    private static final Logger LOG = LoggerFactory.getLogger(CsrSigner.class);

    private static final Set<GeneralName> localhostAttributes = Set.of(
            new GeneralName(dNSName, "localhost"),
            new GeneralName(iPAddress, "127.0.0.1"),
            new GeneralName(iPAddress, "0:0:0:0:0:0:0:1")
    );

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

    private boolean isDNSName(final int name) {
        return name == dNSName;
    }

    private Duration periodToDuration(Period period) {
        return Duration.ofDays(period.getYears() * 365L + period.getMonths() * 30L + period.getDays());
    }

    private Duration safeParse(String duration) {
        try {
            return Duration.parse(duration);
        } catch (DateTimeParseException ignored) {
            return periodToDuration(Period.parse(duration));
        }
    }

    private Instant plusIsoDuration(Instant validFrom, String duration) {
        return validFrom.plus(safeParse(duration));
    }

    public X509Certificate sign(PrivateKey caPrivateKey, X509Certificate caCertificate, PKCS10CertificationRequest csr, RenewalPolicy renewalPolicy) throws Exception {
        Instant validFrom = Instant.now(clock);
        var validUntil = plusIsoDuration(validFrom, renewalPolicy.certificateLifetime());

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

        var builder = new X509v3CertificateBuilder(
                issuerName,
                serialNumber,
                Date.from(validFrom), Date.from(validUntil),
                csr.getSubject(), csr.getSubjectPublicKeyInfo());

        var altNames = Optional.ofNullable(csr.getAttributes(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest))
                .stream()
                .flatMap(Arrays::stream)
                .map(attribute -> Extensions.getInstance(attribute.getAttrValues().getObjectAt(0)))
                .flatMap(extensions -> Optional.ofNullable(GeneralNames.fromExtensions(extensions, Extension.subjectAlternativeName))
                        .flatMap(gns -> Optional.ofNullable(gns.getNames()))
                        .stream()
                        .flatMap(Arrays::stream))
                .filter(name -> isValidName(name.getTagNo()))
                .flatMap(name -> isDNSName(name.getTagNo()) ? resolveDNSName(name) : Stream.of(name))
                .collect(Collectors.toSet());
        LOG.error("XXX altnames: {}", altNames);
        if (!altNames.isEmpty()) {
            builder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(Sets.union(localhostAttributes, altNames).toArray(new GeneralName[0])));
        }

        ContentSigner signer = new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(caPrivateKey);
        X509CertificateHolder certHolder = builder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

    private Stream<? extends GeneralName> resolveDNSName(GeneralName name) {
        final var hostname = name.getName().toString();
        try {
            final var inetAddress = InetAddress.getByName(hostname);
            return Stream.of(name, new GeneralName(iPAddress, inetAddress.getHostAddress()));
        } catch (UnknownHostException e) {
            return Stream.of(name);
        }
    }

}
