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
package org.graylog2.plugin;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.common.net.InetAddresses;
import org.graylog2.inputs.TestHelper;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToolsTest {

    @Test
    public void testGetUriWithPort() throws Exception {
        final URI uriWithPort = new URI("http://example.com:12345");
        final URI httpUriWithoutPort = new URI("http://example.com");
        final URI httpsUriWithoutPort = new URI("https://example.com");
        final URI uriWithUnknownSchemeAndWithoutPort = new URI("foobar://example.com");

        assertEquals(12345, Tools.getUriWithPort(uriWithPort, 1).getPort());
        assertEquals(80, Tools.getUriWithPort(httpUriWithoutPort, 1).getPort());
        assertEquals(443, Tools.getUriWithPort(httpsUriWithoutPort, 1).getPort());
        assertEquals(1, Tools.getUriWithPort(uriWithUnknownSchemeAndWithoutPort, 1).getPort());
    }

    @Test
    public void testGetUriWithScheme() throws Exception {
        assertEquals("gopher", Tools.getUriWithScheme(new URI("http://example.com"), "gopher").getScheme());
        assertNull(Tools.getUriWithScheme(new URI("http://example.com"), null).getScheme());
        assertNull(Tools.getUriWithScheme(null, "http"));
    }

    @Test
    public void testGetPID() {
        String result = Tools.getPID();
        assertTrue(Integer.parseInt(result) > 0);
    }

    @Test
    public void testGetUTCTimestamp() {

        assertTrue(Tools.getUTCTimestamp() > 0);
    }

    @Test
    public void testGetUTCTimestampWithMilliseconds() {

        assertTrue(Tools.getUTCTimestampWithMilliseconds() > 0.0d);
        assertTrue(Tools.getUTCTimestampWithMilliseconds(Instant.now().toEpochMilli()) > 0.0d);
    }

    @Test
    public void testGetLocalHostname() {

        String hostname = Tools.getLocalHostname();

        assertFalse(hostname.isEmpty());
    }

    @Test
    public void testSyslogLevelToReadable() {
        assertEquals("Invalid", Tools.syslogLevelToReadable(1337));
        assertEquals("Emergency", Tools.syslogLevelToReadable(0));
        assertEquals("Critical", Tools.syslogLevelToReadable(2));
        assertEquals("Informational", Tools.syslogLevelToReadable(6));
    }

    @Test
    public void testSyslogFacilityToReadable() {
        assertEquals("Unknown", Tools.syslogFacilityToReadable(9001));
        assertEquals("kernel", Tools.syslogFacilityToReadable(0));
        assertEquals("FTP", Tools.syslogFacilityToReadable(11));
        assertEquals("local6", Tools.syslogFacilityToReadable(22));
    }

    @Test
    public void testGetSystemInformation() {
        String result = Tools.getSystemInformation();
        assertTrue(result.trim().length() > 0);
    }

    @Test
    public void testDecompressZlib() throws IOException {
        final String testString = "Teststring 123";
        final byte[] compressed = TestHelper.zlibCompress(testString);

        assertEquals(testString, Tools.decompressZlib(compressed));
    }

    @Test
    public void testDecompressZlibBomb() throws URISyntaxException, IOException {
        final URL url = Resources.getResource("org/graylog2/plugin/zlib64mb.raw");
        final byte[] testData = Files.readAllBytes(Paths.get(url.toURI()));
        assertThat(Tools.decompressZlib(testData, 1024)).hasSize(1024);
    }

    @Test
    public void testDecompressGzip() throws IOException {
        final String testString = "Teststring 123";
        final byte[] compressed = TestHelper.gzipCompress(testString);

        assertEquals(testString, Tools.decompressGzip(compressed));
    }

    @Test
    public void testDecompressGzipBomb() throws URISyntaxException, IOException {
        final URL url = Resources.getResource("org/graylog2/plugin/gzip64mb.gz");
        final byte[] testData = Files.readAllBytes(Paths.get(url.toURI()));
        assertThat(Tools.decompressGzip(testData, 1024)).hasSize(1024);
    }

    @Test
    public void testDecompressGzipEmptyInput() throws IOException {
        assertThrows(EOFException.class, () ->
                Tools.decompressGzip(new byte[0]));
    }

    /**
     * ruby-1.9.2-p136 :001 > [Time.now.to_i, 2.days.ago.to_i]
     * => [1322063329, 1321890529]
     */
    @Test
    public void testGetTimestampDaysAgo() {
        assertEquals(1321890529, Tools.getTimestampDaysAgo(1322063329, 2));
    }

    @Test
    public void testEncodeBase64() {
        assertEquals("bG9sd2F0LmVuY29kZWQ=", Tools.encodeBase64("lolwat.encoded"));
    }

    @Test
    public void testDecodeBase64() {
        assertEquals("lolwat.encoded", Tools.decodeBase64("bG9sd2F0LmVuY29kZWQ="));
    }

    @Test
    public void testGenerateServerId() {
        String id = Tools.generateServerId();

        /*
         * Make sure it has dashes in it. We need that to build a short ID later.
         * Short version: Everything falls apart if this is not an UUID-style ID.
         */
        assertTrue(id.contains("-"));
    }

    @Test
    public void testAsSortedSet() {
        List<Integer> sortMe = Lists.newArrayList();
        sortMe.add(0);
        sortMe.add(2);
        sortMe.add(6);
        sortMe.add(1);
        sortMe.add(10);
        sortMe.add(25);
        sortMe.add(11);

        SortedSet<Integer> expected = new TreeSet<>();
        expected.add(0);
        expected.add(1);
        expected.add(2);
        expected.add(6);
        expected.add(10);
        expected.add(11);
        expected.add(25);

        assertEquals(expected, Tools.asSortedSet(sortMe));
    }

    @Test
    public void testSafeSubstring() {
        assertNull(Tools.safeSubstring(null, 10, 20));
        assertNull(Tools.safeSubstring("", 10, 20));
        assertNull(Tools.safeSubstring("foo", -1, 2));
        assertNull(Tools.safeSubstring("foo", 1, 0));
        assertNull(Tools.safeSubstring("foo", 5, 2));
        assertNull(Tools.safeSubstring("foo", 1, 1));
        assertNull(Tools.safeSubstring("foo", 2, 1));

        assertEquals("justatest", Tools.safeSubstring("justatest", 0, 9));
        assertEquals("tat", Tools.safeSubstring("justatest", 3, 6));
        assertEquals("just", Tools.safeSubstring("justatest", 0, 4));
        assertEquals("atest", Tools.safeSubstring("justatest", 4, 9));
    }

    @Test
    public void testGetDouble() throws Exception {
        assertEquals(null, Tools.getDouble(null));
        assertEquals(null, Tools.getDouble(""));

        assertEquals(0.0, Tools.getDouble(0), 0);
        assertEquals(1.0, Tools.getDouble(1), 0);
        assertEquals(1.42, Tools.getDouble(1.42), 0);
        assertEquals(9001.0, Tools.getDouble(9001), 0);
        assertEquals(9001.23, Tools.getDouble(9001.23), 0);

        assertEquals(1253453.0, Tools.getDouble((long) 1253453), 0);

        assertEquals(88.0, Tools.getDouble("88"), 0);
        assertEquals(1.42, Tools.getDouble("1.42"), 0);
        assertEquals(null, Tools.getDouble("lol NOT A NUMBER"));

        assertEquals(null, Tools.getDouble(new HashMap<String, String>()));

        assertEquals(42.23, Tools.getDouble(new Object() {
            @Override
            public String toString() {
                return "42.23";
            }
        }), 0);
    }

    @Test
    public void testGetNumberForDifferentFormats() {
        assertEquals(1, Tools.getNumber(1, null).intValue(), 1);
        assertEquals(1.0, Tools.getNumber(1, null).doubleValue(), 0.0);

        assertEquals(42, Tools.getNumber(42.23, null).intValue());
        assertEquals(42.23, Tools.getNumber(42.23, null).doubleValue(), 0.0);

        assertEquals(17, Tools.getNumber("17", null).intValue());
        assertEquals(17.0, Tools.getNumber("17", null).doubleValue(), 0.0);

        assertEquals(23, Tools.getNumber("23.42", null).intValue());
        assertEquals(23.42, Tools.getNumber("23.42", null).doubleValue(), 0.0);

        assertNull(Tools.getNumber(null, null));
        assertNull(Tools.getNumber(null, null));
        assertEquals(1, Tools.getNumber(null, 1).intValue());
        assertEquals(1.0, Tools.getNumber(null, 1).doubleValue(), 0.0);
    }

    @Test
    public void testTimeFormatterWithOptionalMilliseconds() {
        /*
         * We can actually consider this working if it does not throw parser exceptions.
         * Check the toString() representation to make sure though. (using startsWith()
         * to avoid problems on test systems in other time zones, that are not CEST and do
         * not end with a +02:00 or shit.)
         */
        assertTrue(DateTime.parse("2013-09-15 02:21:02", Tools.timeFormatterWithOptionalMilliseconds()).toString().startsWith("2013-09-15T02:21:02.000"));
        assertTrue(DateTime.parse("2013-09-15 02:21:02.123", Tools.timeFormatterWithOptionalMilliseconds()).toString().startsWith("2013-09-15T02:21:02.123"));
        assertTrue(DateTime.parse("2013-09-15 02:21:02.12", Tools.timeFormatterWithOptionalMilliseconds()).toString().startsWith("2013-09-15T02:21:02.120"));
        assertTrue(DateTime.parse("2013-09-15 02:21:02.1", Tools.timeFormatterWithOptionalMilliseconds()).toString().startsWith("2013-09-15T02:21:02.100"));
    }

    @Test
    public void testElasticSearchTimeFormatToISO8601() {
        assertTrue(Tools.elasticSearchTimeFormatToISO8601("2014-07-31 14:21:02.000").equals("2014-07-31T14:21:02.000Z"));
    }

    @Test
    public void testTimeFromDouble() {
        assertTrue(Tools.dateTimeFromDouble(1381076986.306509).toString().startsWith("2013-10-06T"));
        assertTrue(Tools.dateTimeFromDouble(1381076986).toString().startsWith("2013-10-06T"));
        assertTrue(Tools.dateTimeFromDouble(1381079085.6).toString().startsWith("2013-10-06T"));
        assertTrue(Tools.dateTimeFromDouble(1381079085.06).toString().startsWith("2013-10-06T"));
    }

    @Test
    public void uriWithTrailingSlashReturnsNullIfURIIsNull() {
        assertNull(Tools.uriWithTrailingSlash(null));
    }

    @Test
    public void uriWithTrailingSlashReturnsURIWithTrailingSlashIfTrailingSlashIsMissing() throws URISyntaxException {
        final String uri = "http://example.com/api/";
        assertEquals(URI.create(uri), Tools.uriWithTrailingSlash(URI.create("http://example.com/api")));
    }

    @Test
    public void uriWithTrailingSlashReturnsURIIfTrailingSlashIsPresent() {
        final URI uri = URI.create("http://example.com/api/");
        assertEquals(uri, Tools.uriWithTrailingSlash(uri));
    }

    @Test
    public void normalizeURIAddsSchemaAndPortAndPathWithTrailingSlash() {
        final URI uri = URI.create("foobar://example.com");
        assertEquals(URI.create("quux://example.com:1234/foobar/"), Tools.normalizeURI(uri, "quux", 1234, "/foobar"));
    }

    @Test
    public void normalizeURIReturnsNormalizedURI() {
        final URI uri = URI.create("foobar://example.com//foo/////bar");
        assertEquals(URI.create("quux://example.com:1234/foo/bar/"), Tools.normalizeURI(uri, "quux", 1234, "/baz"));
    }

    @Test
    public void normalizeURIReturnsNullIfURIIsNull() {
        assertNull(Tools.normalizeURI(null, "http", 1234, "/baz"));
    }

    @Test
    public void isWildcardAddress() {
        assertTrue(Tools.isWildcardInetAddress(InetAddresses.forString("0.0.0.0")));
        assertTrue(Tools.isWildcardInetAddress(InetAddresses.forString("::")));
        assertFalse(Tools.isWildcardInetAddress(null));
        assertFalse(Tools.isWildcardInetAddress(InetAddresses.forString("127.0.0.1")));
        assertFalse(Tools.isWildcardInetAddress(InetAddresses.forString("::1")));
        assertFalse(Tools.isWildcardInetAddress(InetAddresses.forString("198.51.100.23")));
        assertFalse(Tools.isWildcardInetAddress(InetAddresses.forString("2001:DB8::42")));
    }

    @Test
    public void percentageOfRoundedNormalCases() {
        assertEquals(0, Tools.percentageOfRounded(100, 0));
        assertEquals(1, Tools.percentageOfRounded(100, 1));
        assertEquals(50, Tools.percentageOfRounded(100, 50));
        assertEquals(99, Tools.percentageOfRounded(100, 99));
        assertEquals(100, Tools.percentageOfRounded(100, 100));
        assertEquals(25, Tools.percentageOfRounded(200, 50));
        assertEquals(33, Tools.percentageOfRounded(300, 100));
    }

    @Test
    public void percentageOfRoundedWithZeroTotal() {
        assertEquals(0, Tools.percentageOfRounded(0, 0));
        assertEquals(0, Tools.percentageOfRounded(0, 50));
    }

    @Test
    public void percentageOfRoundedWithNegativeValues() {
        assertEquals(0, Tools.percentageOfRounded(-100, 50));
        assertEquals(0, Tools.percentageOfRounded(100, -50));
        assertEquals(0, Tools.percentageOfRounded(-100, -50));
    }

    @Test
    public void percentageOfRoundedWhenValueExceedsTotal() {
        assertEquals(100, Tools.percentageOfRounded(100, 150));
        assertEquals(100, Tools.percentageOfRounded(50, 100));
    }

    @Test
    public void percentageOfRoundedWithLargeValues() {
        // Test with large long values to ensure no overflow
        assertEquals(50, Tools.percentageOfRounded(Long.MAX_VALUE, Long.MAX_VALUE / 2));
        assertEquals(75, Tools.percentageOfRounded(1000000000000L, 750000000000L));
    }

    @Test
    public void percentageOfRoundedRoundingBehavior() {
        // Test rounding behavior (Math.round)
        assertEquals(33, Tools.percentageOfRounded(3, 1)); // 33.33... -> 33
        assertEquals(67, Tools.percentageOfRounded(3, 2)); // 66.66... -> 67
        assertEquals(100, Tools.percentageOfRounded(1000, 999)); // 99.9 -> 100
    }

    @Test
    public void percentageOfNormalCases() {
        assertEquals(0.0, Tools.percentageOf(100, 0), 0.0);
        assertEquals(1.0, Tools.percentageOf(100, 1), 0.0);
        assertEquals(50.0, Tools.percentageOf(100, 50), 0.0);
        assertEquals(99.0, Tools.percentageOf(100, 99), 0.0);
        assertEquals(100.0, Tools.percentageOf(100, 100), 0.0);
        assertEquals(25.0, Tools.percentageOf(200, 50), 0.0);
        assertEquals(33.333333333333336, Tools.percentageOf(300, 100), 0.000001);
    }

    @Test
    public void percentageOfWithZeroTotal() {
        assertEquals(0.0, Tools.percentageOf(0, 0), 0.0);
        assertEquals(0.0, Tools.percentageOf(0, 50), 0.0);
    }

    @Test
    public void percentageOfWithNegativeValues() {
        assertEquals(0.0, Tools.percentageOf(-100, 50), 0.0);
        assertEquals(0.0, Tools.percentageOf(100, -50), 0.0);
        assertEquals(0.0, Tools.percentageOf(-100, -50), 0.0);
    }

    @Test
    public void percentageOfWhenValueExceedsTotal() {
        assertEquals(100.0, Tools.percentageOf(100, 150), 0.0);
        assertEquals(100.0, Tools.percentageOf(50, 100), 0.0);
    }

    @Test
    public void percentageOfWithLargeValues() {
        // Test with large long values to ensure no overflow
        assertEquals(50.0, Tools.percentageOf(Long.MAX_VALUE, Long.MAX_VALUE / 2), 0.1);
        assertEquals(75.0, Tools.percentageOf(1000000000000L, 750000000000L), 0.0);
    }

    @Test
    public void percentageOfPrecision() {
        // Test that double precision is maintained
        assertEquals(33.333333333333336, Tools.percentageOf(3, 1), 0.000001);
        assertEquals(66.66666666666667, Tools.percentageOf(3, 2), 0.000001);
        assertEquals(99.9, Tools.percentageOf(1000, 999), 0.0);
        assertEquals(12.5, Tools.percentageOf(8, 1), 0.0);
        assertEquals(0.1, Tools.percentageOf(1000, 1), 0.0);
    }

    @Test
    public void percentageOfComparedToRounded() {
        // Verify that percentageOf returns precise values while percentageOfRounded rounds
        double precise = Tools.percentageOf(3, 1);
        int rounded = Tools.percentageOfRounded(3, 1);
        assertThat(precise).isEqualTo(33.333333333333336);
        assertThat(rounded).isEqualTo(33);

        precise = Tools.percentageOf(3, 2);
        rounded = Tools.percentageOfRounded(3, 2);
        assertThat(precise).isEqualTo(66.66666666666667);
        assertThat(rounded).isEqualTo(67);

        // Test edge case near 0.5 boundary
        precise = Tools.percentageOf(1000, 995);
        rounded = Tools.percentageOfRounded(1000, 995);
        assertThat(precise).isEqualTo(99.5);
        assertThat(rounded).isEqualTo(100);
    }
}
