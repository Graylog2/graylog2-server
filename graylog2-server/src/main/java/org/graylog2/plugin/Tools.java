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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Doubles;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Utility class for various tool/helper functions.
 */
public final class Tools {
    private static final byte[] EMPTY_BYTE_ARRAY_4 = {0,0,0,0};
    private static final byte[] EMPTY_BYTE_ARRAY_16 = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

    private static final String ES_DATE_FORMAT_JODA = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String ES_DATE_FORMAT_NO_MS = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter ES_DATE_FORMAT_FORMATTER = DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT_JODA).withZoneUTC();
    public static final DateTimeFormatter ES_DATE_FORMAT_NO_MS_FORMATTER = DateTimeFormat.forPattern(Tools.ES_DATE_FORMAT_NO_MS).withZoneUTC();
    public static final DateTimeFormatter ISO_DATE_FORMAT_FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Tools() {
    }

    /**
     * Get the own PID of this process.
     *
     * @return PID of the running process
     */
    public static String getPID() {
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    /**
     * Converts integer syslog loglevel to human readable string
     *
     * @param level The level to convert
     * @return The human readable level
     */
    public static String syslogLevelToReadable(int level) {
        switch (level) {
            case 0:
                return "Emergency";
            case 1:
                return "Alert";
            case 2:
                return "Critical";
            case 3:
                return "Error";
            case 4:
                return "Warning";
            case 5:
                return "Notice";
            case 6:
                return "Informational";
            case 7:
                return "Debug";
        }

        return "Invalid";
    }

    /**
     * Converts integer syslog facility to human readable string
     *
     * @param facility The facility to convert
     * @return The human readable facility
     */
    public static String syslogFacilityToReadable(int facility) {
        switch (facility) {
            case 0:
                return "kernel";
            case 1:
                return "user-level";
            case 2:
                return "mail";
            case 3:
                return "system daemon";
            case 4:
            case 10:
                return "security/authorization";
            case 5:
                return "syslogd";
            case 6:
                return "line printer";
            case 7:
                return "network news";
            case 8:
                return "UUCP";
            case 9:
            case 15:
                return "clock";
            case 11:
                return "FTP";
            case 12:
                return "NTP";
            case 13:
                return "log audit";
            case 14:
                return "log alert";

            // TODO: Make user definable?
            case 16:
                return "local0";
            case 17:
                return "local1";
            case 18:
                return "local2";
            case 19:
                return "local3";
            case 20:
                return "local4";
            case 21:
                return "local5";
            case 22:
                return "local6";
            case 23:
                return "local7";
        }

        return "Unknown";
    }

    /**
     * Get a String containing version information of JRE, OS, ...
     *
     * @return Descriptive string of JRE and OS
     */
    public static String getSystemInformation() {
        String ret = System.getProperty("java.vendor");
        ret += " " + System.getProperty("java.version");
        ret += " on " + System.getProperty("os.name");
        ret += " " + System.getProperty("os.version");
        return ret;
    }

    /**
     * Decompress ZLIB (RFC 1950) compressed data
     *
     * @param compressedData A byte array containing the ZLIB-compressed data.
     * @return A string containing the decompressed data
     */
    public static String decompressZlib(byte[] compressedData) throws IOException {
        return decompressZlib(compressedData, Long.MAX_VALUE);
    }

    /**
     * Decompress ZLIB (RFC 1950) compressed data
     *
     * @param compressedData A byte array containing the ZLIB-compressed data.
     * @param maxBytes       The maximum number of uncompressed bytes to read.
     * @return A string containing the decompressed data
     */
    public static String decompressZlib(byte[] compressedData, long maxBytes) throws IOException {
        try (final ByteArrayInputStream dataStream = new ByteArrayInputStream(compressedData);
             final InflaterInputStream in = new InflaterInputStream(dataStream);
             final InputStream limited = ByteStreams.limit(in, maxBytes)) {
            return new String(ByteStreams.toByteArray(limited), StandardCharsets.UTF_8);
        }
    }

    /**
     * Decompress GZIP (RFC 1952) compressed data
     *
     * @param compressedData A byte array containing the GZIP-compressed data.
     * @return A string containing the decompressed data
     */
    public static String decompressGzip(byte[] compressedData) throws IOException {
        return decompressGzip(compressedData, Long.MAX_VALUE);
    }

    /**
     * Decompress GZIP (RFC 1952) compressed data
     *
     * @param compressedData A byte array containing the GZIP-compressed data.
     * @param maxBytes       The maximum number of uncompressed bytes to read.
     * @return A string containing the decompressed data
     */
    public static String decompressGzip(byte[] compressedData, long maxBytes) throws IOException {
        try (final ByteArrayInputStream dataStream = new ByteArrayInputStream(compressedData);
             final GZIPInputStream in = new GZIPInputStream(dataStream);
             final InputStream limited = ByteStreams.limit(in, maxBytes)) {
            return new String(ByteStreams.toByteArray(limited), StandardCharsets.UTF_8);
        }
    }

    /**
     * @return The current UTC UNIX timestamp.
     */
    public static int getUTCTimestamp() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * Get the current UNIX epoch with milliseconds of the system
     *
     * @return The current UTC UNIX timestamp with milliseconds.
     */
    public static double getUTCTimestampWithMilliseconds() {
        return getUTCTimestampWithMilliseconds(System.currentTimeMillis());
    }

    /**
     * Get the UNIX epoch with milliseconds of the provided millisecond timestamp
     *
     * @param timestamp a millisecond timestamp (milliseconds since UNIX epoch)
     * @return The current UTC UNIX timestamp with milliseconds.
     */
    public static double getUTCTimestampWithMilliseconds(long timestamp) {
        return timestamp / 1000.0;
    }

    @SuppressForbidden("Deliberate invocation")
    public static String getLocalHostname() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            return "Unknown";
        }

        return addr.getHostName();
    }

    @SuppressForbidden("Deliberate invocation")
    public static String getLocalCanonicalHostname() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            return "Unknown";
        }

        return addr.getCanonicalHostName();
    }

    public static int getTimestampDaysAgo(int ts, int days) {
        return (ts - (days * 86400));
    }

    public static String encodeBase64(final String what) {
        return BaseEncoding.base64().encode(what.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeBase64(final String what) {
        return new String(BaseEncoding.base64().decode(what), StandardCharsets.UTF_8);
    }

    @SuppressForbidden("Deliberate invocation")
    public static String rdnsLookup(InetAddress socketAddress) throws UnknownHostException {
        return socketAddress.getCanonicalHostName();
    }

    public static String generateServerId() {
        return UUID.randomUUID().toString();
    }

    public static <T extends Comparable<? super T>> SortedSet<T> asSortedSet(Collection<T> c) {
        return ImmutableSortedSet.copyOf(c);
    }

    public static String buildElasticSearchTimeFormat(DateTime timestamp) {
        return timestamp.toString(ES_DATE_FORMAT_FORMATTER);
    }

    /**
     * The double representation of a UNIX timestamp with milliseconds is a strange, human readable format.
     * <p/>
     * This sucks and no format should use the double representation. Change GELF to use long. (zomg)
     */
    public static DateTime dateTimeFromDouble(double x) {
        return new DateTime(Math.round(x * 1000), DateTimeZone.UTC);
    }

    /**
     * Parse the string representation of an ISO 8601 date/timestamp with milliseconds and timezone.
     */
    public static DateTime dateTimeFromString(String s) {
        return ISO_DATE_FORMAT_FORMATTER.parseDateTime(s);
    }

    /**
     * Accepts our ElasticSearch time formats without milliseconds.
     *
     * @return A DateTimeFormatter suitable to parse an ES_DATE_FORMAT formatted string to a
     * DateTime Object even if it contains no milliseconds.
     */
    public static DateTimeFormatter timeFormatterWithOptionalMilliseconds() {
        // This is the .SSS part
        DateTimeParser ms = new DateTimeFormatterBuilder()
                .appendLiteral(".")
                .appendFractionOfSecond(1, 3)
                .toParser();

        return new DateTimeFormatterBuilder()
                .append(DateTimeFormat.forPattern(ES_DATE_FORMAT_NO_MS).withZoneUTC())
                .appendOptional(ms)
                .toFormatter();
    }

    public static DateTime nowUTC() {
        return new DateTime(DateTimeZone.UTC);
    }

    public static DateTime now(DateTimeZone dateTimeZone) {
        return new DateTime(dateTimeZone);
    }

    /**
     * @return The current date with timezone UTC.
     * @deprecated Use {@link #nowUTC()} instead.
     */
    @Deprecated
    public static DateTime iso8601() {
        return nowUTC();
    }

    public static String getISO8601String(DateTime time) {
        return ISODateTimeFormat.dateTime().print(time);
    }

    /**
     * Try to parse a date in ES_DATE_FORMAT format considering it is in UTC and convert it to an ISO8601 date.
     * If an error is encountered in the process, it will return the original string.
     */
    public static String elasticSearchTimeFormatToISO8601(String time) {
        try {
            DateTime dt = DateTime.parse(time, ES_DATE_FORMAT_FORMATTER);
            return getISO8601String(dt);
        } catch (IllegalArgumentException e) {
            return time;
        }
    }

    /**
     * @param target String to cut.
     * @param start  Character position to start cutting at. Inclusive.
     * @param end    Character position to stop cutting at. Exclusive!
     * @return Extracted/cut part of the string or null when invalid positions where provided.
     */
    public static String safeSubstring(String target, int start, int end) {
        if (target == null) {
            return null;
        }

        int slen = target.length();
        if (start < 0 || end <= 0 || end <= start || slen < start || slen < end) {
            return null;
        }

        return target.substring(start, end);
    }

    /**
     * Convert something to a double in a fast way having a good guess
     * that it is a double. This is perfect for MongoDB data that *should*
     * have been stored as doubles already so there is a high probability
     * of easy converting.
     *
     * @param x The object to convert to a double
     * @return Converted object, 0 if empty or something went wrong.
     */
    public static Double getDouble(Object x) {
        if (x == null) {
            return null;
        }

        if (x instanceof Double) {
            return (Double) x;
        }

        if (x instanceof String) {
            String s = x.toString();
            if (s == null || s.isEmpty()) {
                return null;
            }
        }

        /*
         * This is the last and probably expensive fallback. This should be avoided by
         * only passing in Doubles, Integers, Longs or stuff that can be parsed from it's String
         * representation. You might have to build cached objects that did a safe conversion
         * once for example. There is no way around for the actual values we compare if the
         * user sent them in as non-numerical type.
         */
        return Doubles.tryParse(x.toString());
    }

    public static Number getNumber(Object o, Number defaultValue) {
        if (o instanceof Number) {
            return (Number)o;
        }

        try {
            return Double.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Try to get the primary {@link InetAddress} of the primary network interface with
     * fallback to the local loopback address (usually {@code 127.0.0.1} or {@code ::1}.
     *
     * @return The primary {@link InetAddress} of the primary network interface
     * or the loopback address as fallback.
     * @throws SocketException if the list of network interfaces couldn't be retrieved
     */
    public static InetAddress guessPrimaryNetworkAddress(boolean preferIPv4) throws SocketException {
        final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

        if (interfaces != null) {
            for (NetworkInterface interf : Collections.list(interfaces)) {
                if (!interf.isLoopback() && interf.isUp()) {
                    // Interface is not loopback and up. Try to get the first address.
                    for (InetAddress addr : Collections.list(interf.getInetAddresses())) {
                        if (preferIPv4 && addr instanceof Inet4Address) {
                            return addr;
                        }
                        if (!preferIPv4 && addr instanceof Inet6Address) {
                            return addr;
                        }
                    }
                }
            }
        }

        return InetAddress.getLoopbackAddress();
    }

    public static boolean isWildcardInetAddress(@Nullable InetAddress inetAddress) {
        return inetAddress != null && (Arrays.equals(EMPTY_BYTE_ARRAY_4, inetAddress.getAddress()) || Arrays.equals(EMPTY_BYTE_ARRAY_16, inetAddress.getAddress()));
    }

    @Nullable
    public static URI getUriWithPort(@Nullable final URI uri, final int port) {
        if (uri == null) {
            return null;
        }

        try {
            if (uri.getPort() == -1) {
                final int realPort;
                switch (uri.getScheme()) {
                    case "http":
                        realPort = 80;
                        break;
                    case "https":
                        realPort = 443;
                        break;
                    default:
                        realPort = port;
                }

                return new URI(
                        uri.getScheme(),
                        uri.getUserInfo(),
                        uri.getHost(),
                        realPort,
                        uri.getPath(),
                        uri.getQuery(),
                        uri.getFragment());
            }

            return uri;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not parse URI.", e);
        }
    }

    @Nullable
    public static URI getUriWithScheme(@Nullable final URI uri, final String scheme) {
        if (uri == null) {
            return null;
        }

        try {
            return new URI(
                    scheme,
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not parse URI.", e);
        }
    }

    @Nullable
    public static URI getUriWithDefaultPath(@Nullable final URI uri, final String path) {
        if (uri == null) {
            return null;
        }

        try {
            return new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    isNullOrEmpty(uri.getPath()) ? path : uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not parse URI.", e);
        }
    }

    @Nullable
    public static URI uriWithTrailingSlash(@Nullable final URI uri) {
        if (uri == null) {
            return null;
        }

        final String path = firstNonNull(uri.getPath(), "/");
        if(path.endsWith("/")) {
            return uri;
        } else {
            try {
                return new URI(
                        uri.getScheme(),
                        uri.getUserInfo(),
                        uri.getHost(),
                        uri.getPort(),
                        path + "/",
                        uri.getQuery(),
                        uri.getFragment());
            } catch (URISyntaxException e) {
                throw new RuntimeException("Could not parse URI.", e);
            }
        }
    }

    @Nullable
    public static URI normalizeURI(@Nullable final URI uri, String scheme, int port, String path) {
        return Optional.ofNullable(uri)
                .map(u -> getUriWithScheme(u, scheme))
                .map(u -> getUriWithPort(u, port))
                .map(u -> getUriWithDefaultPath(u, path))
                .map(Tools::uriWithTrailingSlash)
                .map(URI::normalize)
                .orElse(null);
    }

    @Nullable
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String bytesToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
            sb.append(String.format(Locale.ENGLISH, "%02x", b & 0xff)).append(' ');
        return sb.toString().trim();
    }

    /**
     * The default uncaught exception handler will print to STDERR, which we don't always want for threads.
     * Using this utility method you can avoid writing to STDERR on a per-thread basis
     */
    public static void silenceUncaughtExceptionsInThisThread() {
        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread ignored, Throwable ignored1) {
            }
        });
    }

    public static class LogUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Logger log;

        public LogUncaughtExceptionHandler(Logger log) {
            this.log = log;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            log.error("Thread {} failed by not catching exception: {}.", t.getName(), e);
        }
    }

    public static Optional<AbsoluteRange> extractHistogramBoundaries(final String query) {
        try {
            final JsonParser jp = OBJECT_MAPPER.getFactory().createParser(query);
            final JsonNode rootNode = OBJECT_MAPPER.readTree(jp);
            if (rootNode == null) {
                throw new IOException("null result");
            }
            final JsonNode timestampNode = rootNode.findValue("range").findValue("timestamp");
            final String from = elasticSearchTimeFormatToISO8601(timestampNode.findValue("from").asText());
            final String to = elasticSearchTimeFormatToISO8601(timestampNode.findValue("to").asText());

            return Optional.of(AbsoluteRange.create(from, to));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
