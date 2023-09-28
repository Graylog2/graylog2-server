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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;

class CsrSignerTest {
    private static final X500Name subjectName = new X500Name("CN=Example Request");
    private static final Instant fixedInstant = Instant.parse("2023-09-28T12:50:00Z");
    private static final Clock fixedClock = Clock.fixed(fixedInstant, UTC);


    @BeforeEach
    void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private X509Certificate sign(String lifetime) throws Exception {
        var keyPair = createPrivateKey();
        var cert = createCert(keyPair);
        var privateKey = keyPair.getPrivate();
        var csr = createCSR(keyPair);

        return new CsrSigner(fixedClock).sign(privateKey, cert, csr, new RenewalPolicy(RenewalPolicy.Mode.AUTOMATIC, lifetime));
    }

    @Test
    void testSigningCertWithTwoHoursLifetime() throws Exception {
        var result = sign("PT2H");
        assertThat(result).isNotNull();
        assertThat(result.getNotAfter()).isEqualTo(fixedInstant.plus(2, ChronoUnit.HOURS));
    }

    @Test
    void testSigningCertWithSixMonthsLifetime() throws Exception {
        var result = sign("P6M");
        assertThat(result).isNotNull();
        assertThat(result.getNotAfter()).isEqualTo(fixedInstant.plus(180, ChronoUnit.DAYS));
    }

    private PKCS10CertificationRequest createCSR(KeyPair keyPair) throws OperatorCreationException {
        var contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        var pkcs10Builder = new PKCS10CertificationRequestBuilder(subjectName, SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));

        return pkcs10Builder.build(contentSigner);
    }

    private X509Certificate createCert(KeyPair keyPair) throws OperatorCreationException, CertificateException {
        var startDate = Date.from(Instant.now());
        var endDate = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        var serialNumber = new BigInteger(128, new SecureRandom());

        var certBuilder = new X509v3CertificateBuilder(
                subjectName,
                serialNumber,
                startDate,
                endDate,
                subjectName,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
        );

        var privateKey = keyPair.getPrivate();
        var certHolder = certBuilder.build(
                new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256WithRSA")
                        .setProvider("BC")
                        .build(privateKey)
        );

        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    private KeyPair createPrivateKey() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");

        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }
}
