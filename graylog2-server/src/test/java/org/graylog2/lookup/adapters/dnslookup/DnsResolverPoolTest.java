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
package org.graylog2.lookup.adapters.dnslookup;

import org.graylog2.lookup.adapters.dnslookup.DnsResolverPool.ResolverLease;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(MockitoJUnitRunner.class)
class DnsResolverPoolTest {

    private static final int POOL_SIZE = 10;

    @Test
    public void testResolverIndex() {
        final DnsResolverPool dnsResolverPool = new DnsResolverPool("", 100, POOL_SIZE, 300);
        dnsResolverPool.initialize();
        assertEquals(POOL_SIZE, dnsResolverPool.poolSize());
        final int randomResolverIndex = dnsResolverPool.randomResolverIndex();
        assertTrue(randomResolverIndex >= 0);
        assertTrue(randomResolverIndex < POOL_SIZE);
        dnsResolverPool.stop();
    }

    @Test
    public void verifyRefresh() throws InterruptedException {
        final DnsResolverPool dnsResolverPool = new DnsResolverPool("", 100, 1, 1);
        dnsResolverPool.initialize();
        final ResolverLease nextLease = dnsResolverPool.takeLease();
        final String initialLeaseId = nextLease.getId();
        dnsResolverPool.returnLease(nextLease);
        Thread.sleep(1500);
        final ResolverLease finalLease = dnsResolverPool.takeLease();
        assertNotEquals(initialLeaseId, finalLease.getId());
        dnsResolverPool.stop();
    }
}
