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

import org.bouncycastle.operator.OperatorCreationException;
import org.graylog.events.JobSchedulerTestClock;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CertRenewalServiceTest {
    private Date notAfter(final DateTime now, final int subtractMinutes, final int addMinutes) {
        return now.plusMinutes(addMinutes).minusMinutes(subtractMinutes).toDate();
    }

    @Test
    public void testCertRenewalCalculationDuration() throws CertificateException, NoSuchAlgorithmException, OperatorCreationException {
        final var now = DateTime.now(DateTimeZone.UTC);
        final JobSchedulerClock clock = new JobSchedulerTestClock(now);
        final var nextRun = clock.nowUTC().plusMinutes(30);
        final var service = new CertRenewalServiceImpl(null, null, null, null, clock, null);
        // 2 hours is our smalles interval in the FE, so I'm testing only small intervals and expect larger intervals to work accordingly
        // with a threshold of 10%, a cert should either be invalid if it's no longer valid in 12min (10% of 120min) or if it get's invalid until the next run of the cert checker
        final var policy = new RenewalPolicy(RenewalPolicy.Mode.MANUAL, "PT2H");

        // renewal check every 30min, so the following should be still valid
        final var cert1 = notAfter(now, 0, 35);
        assertFalse(service.needsRenewal(nextRun, policy, cert1));

        // renewal check every 30min, threshold also takes it, so the following should be no longer valid
        final var cert2 = notAfter(now, 0, 5);
        assertTrue(service.needsRenewal(nextRun, policy, cert2));

        // renewal check every 30min, threshold is smaller, but the following should be no longer valid
        final var cert3 = notAfter(now, 0, 15);
        assertTrue(service.needsRenewal(nextRun, policy, cert3));

        // renewal check every 30min, threshold is smaller, but the following should be no longer valid
        final var cert4 = notAfter(now, 24*60, 25);
        assertTrue(service.needsRenewal(nextRun, policy, cert4));
    }

    @Test
    public void testCertRenewalCalculationPeriod() throws CertificateException, NoSuchAlgorithmException, OperatorCreationException {
        final var now = DateTime.now(DateTimeZone.UTC);
        final JobSchedulerClock clock = new JobSchedulerTestClock(now);
        final var nextRun = clock.nowUTC().plusMinutes(30);
        final var service = new CertRenewalServiceImpl(null, null, null, null, clock, null);
        // Using a date period should also work, so here's the test for that. 1 day is 1440 minutes, so it should need the renewal after 143 minutes
        final var policy = new RenewalPolicy(RenewalPolicy.Mode.MANUAL, "P1D");

        final var cert1 = notAfter(now, 0, 24 * 60);
        assertFalse(service.needsRenewal(nextRun, policy, cert1));

        final var cert2 = notAfter(now, 0, 144);
        assertFalse(service.needsRenewal(nextRun, policy, cert2));

        final var cert3 = notAfter(now, 0, 143);
        assertTrue(service.needsRenewal(nextRun, policy, cert3));

    }


}
