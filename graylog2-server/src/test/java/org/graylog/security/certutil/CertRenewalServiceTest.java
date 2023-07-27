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

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.graylog.security.certutil.CertConstants.KEY_GENERATION_ALGORITHM;
import static org.graylog.security.certutil.CertConstants.SIGNING_ALGORITHM;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CertRenewalServiceTest {
    private X509Certificate generate(final DateTime now, final int subtractMinutes, final int addMinutes) throws OperatorCreationException, CertificateException, NoSuchAlgorithmException {
        final Date notBefore = now.minusMinutes(subtractMinutes).toDate();
        final Date notAfter = now.plusMinutes(addMinutes).toDate();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_GENERATION_ALGORITHM);
        java.security.KeyPair keyPair = keyGen.generateKeyPair();

        final ContentSigner contentSigner = new JcaContentSignerBuilder(SIGNING_ALGORITHM).build(keyPair.getPrivate());
        final X500Name x500Name = new X500Name("CN=graylog.test");
        final X509v3CertificateBuilder certificateBuilder =
                new JcaX509v3CertificateBuilder(x500Name,
                        BigInteger.valueOf(now.getMillis()),
                        notBefore,
                        notAfter,
                        x500Name,
                        keyPair.getPublic());

        return new JcaX509CertificateConverter()
                .setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));
    }

    @Test
    public void testCertRenewalCalculation() throws CertificateException, NoSuchAlgorithmException, OperatorCreationException {
        final var now = DateTime.now(DateTimeZone.UTC);
        final JobSchedulerClock clock = new JobSchedulerTestClock(now);
        final var nextRun = clock.nowUTC().plusMinutes(30);
        final var service = new CertRenewalServiceImpl(clock);
        // 2 hours is our smalles interval in the FE, so I'm testing only small intervals and expect larger intervals to work accordingly
        // with a threshold of 10%, a cert should either be invalid if it's no longer valid in 12min (10% of 120min) or if it get's invalid until the next run of the cert checker
        final var policy = new RenewalPolicy(RenewalPolicy.Mode.MANUAL, "PT2H");

        // renewal check every 30min, so the following should be still valid
        final var cert1 = generate(now, 0, 35);
        assertFalse(service.needsRenewal(nextRun, policy, cert1));

        // renewal check every 30min, threshold also takes it, so the following should be no longer valid
        final var cert2 = generate(now, 0, 5);
        assertTrue(service.needsRenewal(nextRun, policy, cert2));

        // renewal check every 30min, threshold is smaller, but the following should be no longer valid
        final var cert3 = generate(now, 0, 15);
        assertTrue(service.needsRenewal(nextRun, policy, cert3));

        // renewal check every 30min, threshold is smaller, but the following should be no longer valid
        final var cert4 = generate(now, 24*60, 25);
        assertTrue(service.needsRenewal(nextRun, policy, cert4));
    }
}
