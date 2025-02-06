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
package org.graylog2.utilities;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CIDRPatriciaTrieTest {

    @Test
    public void testLookups() {
        final CIDRPatriciaTrie trie = buildTrie();

        assertThat(trie).satisfies(t -> {
            assertThat(t.longestPrefixRangeLookup("192.168.1.100")).isEqualTo("IPv4 Range 1");
            assertThat(t.longestPrefixRangeLookup("10.0.5.1")).isEqualTo("IPv4 Range 2");
            assertThat(t.longestPrefixRangeLookup("35.139.253.123")).isEqualTo("IPv4 Range 3");
            assertThat(t.longestPrefixRangeLookup("192.168.102.8")).isEqualTo("HR Subnet 1");
            assertThat(t.longestPrefixRangeLookup("192.168.102.22")).isEqualTo("HR Subnet 2");
            assertThat(t.longestPrefixRangeLookup("192.168.102.40")).isEqualTo("HR Subnet 3");
            assertThat(t.longestPrefixRangeLookup("172.16.5.4")).isNull();
            assertThat(t.longestPrefixRangeLookup("2001:db8:abcd::1")).isEqualTo("IPv6 Range 1");
            assertThat(t.longestPrefixRangeLookup("2404:6800:4001:abcd::1")).isEqualTo("IPv6 Range 2");
            assertThat(t.longestPrefixRangeLookup("8dbf:88a6:2000:4ddc:f708:cf8d:f2a5:a420")).isEqualTo("IPv6 Range 3");
            assertThat(t.longestPrefixRangeLookup("77f:8b7a:3e82:6fb3:ba15:9b68:7fe0:a695")).isEqualTo("IPv6 Range 4");
            assertThat(t.longestPrefixRangeLookup("2001:db7::")).isEqualTo("Single IPv6");
            assertThat(t.longestPrefixRangeLookup("2607:f8b0:4001:c01::")).isNull();
        });
    }

    @Test
    public void testRemoval() {
        final CIDRPatriciaTrie trie = buildTrie();

        assertThat(trie.longestPrefixRangeLookup("8dbf:88a6:2000:4ddc:f708:cf8d:f2a5:a420")).isEqualTo("IPv6 Range 3");
        trie.removeCIDR("8dbf:8000::/19");
        // CIDR is no longer in lookup
        assertThat(trie.longestPrefixRangeLookup("8dbf:88a6:2000:4ddc:f708:cf8d:f2a5:a420")).isNull();
        // Confirm other lookups still work
        assertThat(trie.longestPrefixRangeLookup("77f:8b7a:3e82:6fb3:ba15:9b68:7fe0:a695")).isEqualTo("IPv6 Range 4");
        assertThat(trie.longestPrefixRangeLookup("192.168.1.100")).isEqualTo("IPv4 Range 1");
        assertThat(trie.longestPrefixRangeLookup("10.0.5.1")).isEqualTo("IPv4 Range 2");
        assertThat(trie.longestPrefixRangeLookup("35.139.253.123")).isEqualTo("IPv4 Range 3");
        assertThat(trie.longestPrefixRangeLookup("2001:db8:abcd::1")).isEqualTo("IPv6 Range 1");
        assertThat(trie.longestPrefixRangeLookup("2404:6800:4001:abcd::1")).isEqualTo("IPv6 Range 2");
        assertThat(trie.longestPrefixRangeLookup("77f:8b7a:3e82:6fb3:ba15:9b68:7fe0:a695")).isEqualTo("IPv6 Range 4");
    }

    @Test
    public void testBadEntry() {
        final CIDRPatriciaTrie trie = new CIDRPatriciaTrie();
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> trie.insertCIDR("127.a.3.21/12", "Bad Range 1"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> trie.insertCIDR("not.an.ip.address/12", "Bad Range 2"));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> trie.insertCIDR("127.0.0.0", "Bad Range 3"));
    }

    @Test
    public void testCleanCopy() throws InterruptedException {
        final CIDRPatriciaTrie trie = new CIDRPatriciaTrie();
        final long expireAt = DateTime.now(DateTimeZone.UTC).getMillis() + 500L;
        trie.insertCIDR("192.168.1.0/24", "IPv4 Range 1", expireAt);
        trie.insertCIDR("10.0.0.0/8", "IPv4 Range 2", expireAt);
        trie.insertCIDR("35.138.0.0/15", "IPv4 Range 3", expireAt);
        Thread.sleep(1000L);
        final CIDRPatriciaTrie copy = trie.cleanCopy();
        assertThat(copy.isEmpty()).isTrue();
    }

    @Test
    public void testToBinaryIP() {
        String cidrBinary = CIDRPatriciaTrie.toBinaryString("2002:0000:0000:1234:0000:0000:0000:0000", 64);
        assertThat(cidrBinary).isEqualTo("0010000000000010000000000000000000000000000000000001001000110100");
        cidrBinary = CIDRPatriciaTrie.toBinaryString("192.168.103.16", -1);
        assertThat(cidrBinary).isEqualTo("11000000101010000110011100010000");
        cidrBinary = CIDRPatriciaTrie.toBinaryString("2001:db7::", -1);
        assertThat(cidrBinary).isEqualTo("00100000000000010000110110110111000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
        cidrBinary = CIDRPatriciaTrie.toBinaryString("192.168.102.0", 24);
        assertThat(cidrBinary).isEqualTo("110000001010100001100110");

    }

    private static CIDRPatriciaTrie buildTrie() {
        final CIDRPatriciaTrie trie = new CIDRPatriciaTrie();
        trie.insertCIDR("192.168.1.0/24", "IPv4 Range 1");
        trie.insertCIDR("10.0.0.0/8", "IPv4 Range 2");
        trie.insertCIDR("35.138.0.0/15", "IPv4 Range 3");
        trie.insertCIDR("192.168.102.0/24", "HR");
        trie.insertCIDR("192.168.102.0/28", "HR Subnet 1");
        trie.insertCIDR("192.168.102.16/28", "HR Subnet 2");
        trie.insertCIDR("192.168.102.32/28", "HR Subnet 3");
        trie.insertCIDR("2001:db8::/32", "IPv6 Range 1");
        trie.insertCIDR("2404:6800:4001::/48", "IPv6 Range 2");
        trie.insertCIDR("8dbf:8000::/19", "IPv6 Range 3");
        trie.insertCIDR("77f::/16", "IPv6 Range 4");
        trie.insertCIDR("17c5:b180::/35", "IPv6 Range 5");
        trie.insertCIDR("2001:db7::/128","Single IPv6");
        return trie;
    }
}
