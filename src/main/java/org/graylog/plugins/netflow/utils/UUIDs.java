/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.graylog.plugins.netflow.utils;

import com.google.common.base.Charsets;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility methods to work with UUID and most specifically with time-based ones
 * (version 1).
 */
public final class UUIDs {

    private UUIDs() {};

    // http://www.ietf.org/rfc/rfc4122.txt
    private static final long START_EPOCH = makeEpoch();
    private static final long CLOCK_SEQ_AND_NODE = makeClockSeqAndNode();

    /*
     * The min and max possible lsb for a UUID.
     * Note that his is not 0 and all 1's because Cassandra TimeUUIDType
     * compares the lsb parts as a signed byte array comparison. So the min
     * value is 8 times -128 and the max is 8 times +127.
     *
     * Note that we ignore the uuid variant (namely, MIN_CLOCK_SEQ_AND_NODE
     * have variant 2 as it should, but MAX_CLOCK_SEQ_AND_NODE have variant 0)
     * because I don't trust all uuid implementation to have correctly set
     * those (pycassa don't always for instance).
     */
    private static final long MIN_CLOCK_SEQ_AND_NODE = 0x8080808080808080L;
    private static final long MAX_CLOCK_SEQ_AND_NODE = 0x7f7f7f7f7f7f7f7fL;

    private static final AtomicLong lastTimestamp = new AtomicLong(0L);

    private static long makeEpoch() {
        // UUID v1 timestamp must be in 100-nanoseconds interval since 00:00:00.000 15 Oct 1582.
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-0"));
        c.set(Calendar.YEAR, 1582);
        c.set(Calendar.MONTH, Calendar.OCTOBER);
        c.set(Calendar.DAY_OF_MONTH, 15);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static long makeNode() {

        /*
         * We don't have access to the MAC address (in pure JAVA at least) but
         * need to generate a node part that identify this host as uniquely as
         * possible.
         * The spec says that one option is to take as many source that
         * identify this node as possible and hash them together. That's what
         * we do here by gathering all the ip of this host as well as a few
         * other sources.
         */
        try {

            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (String address : getAllLocalAddresses())
                update(digest, address);

            Properties props = System.getProperties();
            update(digest, props.getProperty("java.vendor"));
            update(digest, props.getProperty("java.vendor.url"));
            update(digest, props.getProperty("java.version"));
            update(digest, props.getProperty("os.arch"));
            update(digest, props.getProperty("os.name"));
            update(digest, props.getProperty("os.version"));

            byte[] hash = digest.digest();

            long node = 0;
            for (int i = 0; i < 6; i++)
                node |= (0x00000000000000ffL & (long)hash[i]) << (i*8);
            // Since we don't use the mac address, the spec says that multicast
            // bit (least significant bit of the first byte of the node ID) must be 1.
            return node | 0x0000010000000000L;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void update(MessageDigest digest, String value) {
        if (value != null)
            digest.update(value.getBytes(Charsets.UTF_8));
    }

    private static long makeClockSeqAndNode() {
        long clock = new Random(System.currentTimeMillis()).nextLong();
        long node = makeNode();

        long lsb = 0;
        lsb |= (clock & 0x0000000000003FFFL) << 48;
        lsb |= 0x8000000000000000L;
        lsb |= node;
        return lsb;
    }

    /**
     * Creates a new random (version 4) UUID.
     * <p>
     * This method is just a convenience for {@code UUID.randomUUID()}.
     *
     * @return a newly generated, pseudo random, version 4 UUID.
     */
    public static UUID random() {
        return UUID.randomUUID();
    }

    /**
     * Creates a new time-based (version 1) UUID.
     * <p>
     * UUID generated by this method are suitable for use with the
     * {@code timeuuid} Cassandra type. In particular the generated UUID
     * includes the timestamp of its generation.
     *
     * @return a new time-based UUID.
     */
    public static UUID timeBased() {
        return new UUID(makeMSB(getCurrentTimestamp()), CLOCK_SEQ_AND_NODE);
    }

    /**
     * Creates a "fake" time-based UUID that sorts as the smallest possible
     * version 1 UUID generated at the provided timestamp.
     * <p>
     * Such created UUID are useful in queries to select a time range of a
     * {@code timeuuid} column.
     * <p>
     * The UUID created by this method <b>are not unique</b> and as such are
     * <b>not</b> suitable for anything else than querying a specific time
     * range. In particular, you should not insert such UUID.
     * <p>
     * Also, the timestamp to provide as parameter must be a unix timestamp (as
     * returned by {@link System#currentTimeMillis} or {@link java.util.Date#getTime}),
     * not a UUID 100-nanoseconds intervals since 15 October 1582. In other
     * words, given a UUID {@code uuid}, you should never do
     * {@code startOf(uuid.timestamp())} but rather
     * {@code startOf(unixTimestamp(uuid.timestamp()))}.
     * <p>
     * Lastly, please note that Cassandra's timeuuid sorting is not compatible
     * with {@link UUID#compareTo} and hence the UUID created by this method
     * are not necessarily lower bound for that latter method.
     *
     * @param timestamp the unix timestamp for which the created UUID must be a
     * lower bound.
     * @return the smallest (for Cassandra timeuuid sorting) UUID of {@code timestamp}.
     */
    public static UUID startOf(long timestamp) {
        return new UUID(makeMSB(fromUnixTimestamp(timestamp)), MIN_CLOCK_SEQ_AND_NODE);
    }

    /**
     * Creates a "fake" time-based UUID that sorts as the biggest possible
     * version 1 UUID generated at the provided timestamp.
     * <p>
     * Such created UUID are useful in queries to select a time range of a
     * {@code timeuuid} column.
     * <p>
     * The UUID created by this method <b>are not unique</b> and as such are
     * <b>not</b> suitable for anything else than querying a specific time
     * range. In particular, you should not insert such UUID.
     * <p>
     * Also, the timestamp to provide as parameter must be a unix timestamp (as
     * returned by {@link System#currentTimeMillis} or {@link java.util.Date#getTime}),
     * not a UUID 100-nanoseconds intervals since 15 October 1582. In other
     * words, given a UUID {@code uuid}, you should never do
     * {@code startOf(uuid.timestamp())} but rather
     * {@code startOf(unixTimestamp(uuid.timestamp()))}.
     * <p>
     * Lastly, please note that Cassandra's timeuuid sorting is not compatible
     * with {@link UUID#compareTo} and hence the UUID created by this method
     * are not necessarily upper bound for that latter method.
     *
     * @param timestamp the unix timestamp for which the created UUID must be an
     * upper bound.
     * @return the biggest (for Cassandra timeuuid sorting) UUID of {@code timestamp}.
     */
    public static UUID endOf(long timestamp) {
        long uuidTstamp = fromUnixTimestamp(timestamp + 1) - 1;
        return new UUID(makeMSB(uuidTstamp), MAX_CLOCK_SEQ_AND_NODE);
    }

    /**
     * Return the unix timestamp contained by the provided time-based UUID.
     * <p>
     * This method is not equivalent to {@code uuid.timestamp()}.  More
     * precisely, a version 1 UUID stores a timestamp that represents the
     * number of 100-nanoseconds intervals since midnight, 15 October 1582 and
     * that is what {@code uuid.timestamp()} returns. This method however
     * converts that timestamp to the equivalent unix timestamp in
     * milliseconds, i.e. a timestamp representing a number of milliseconds
     * since midnight, January 1, 1970 UTC. In particular the timestamps
     * returned by this method are comparable to the timestamp returned by
     * {@link System#currentTimeMillis}, {@link java.util.Date#getTime}, etc.
     *
     * @param uuid the UUID to return the timestamp of.
     * @return the unix timestamp of {@code uuid}.
     *
     * @throws IllegalArgumentException if {@code uuid} is not a version 1 UUID.
     */
    public static long unixTimestamp(UUID uuid) {
        if (uuid.version() != 1)
            throw new IllegalArgumentException(String.format("Can only retrieve the unix timestamp for version 1 uuid (provided version %d)", uuid.version()));

        long timestamp = uuid.timestamp();
        return (timestamp / 10000) + START_EPOCH;
    }

    /*
     * Note that currently we use System.currentTimeMillis() for a base time in
     * milliseconds, and then if we are in the same milliseconds that the
     * previous generation, we increment the number of nanoseconds.
     * However, since the precision is 100-nanoseconds intervals, we can only
     * generate 10K UUID within a millisecond safely. If we detect we have
     * already generated that much UUID within a millisecond (which, while
     * admittedly unlikely in a real application, is very achievable on even
     * modest machines), then we stall the generator (busy spin) until the next
     * millisecond as required by the RFC.
     */
    private static long getCurrentTimestamp() {
        while (true) {
            long now = fromUnixTimestamp(System.currentTimeMillis());
            long last = lastTimestamp.get();
            if (now > last) {
                if (lastTimestamp.compareAndSet(last, now))
                    return now;
            } else {
                long lastMillis = millisOf(last);
                // If the clock went back in time, bail out
                if (millisOf(now) < millisOf(last))
                    return lastTimestamp.incrementAndGet();

                long candidate = last + 1;
                // If we've generated more than 10k uuid in that millisecond,
                // we restart the whole process until we get to the next millis.
                // Otherwise, we try use our candidate ... unless we've been
                // beaten by another thread in which case we try again.
                if (millisOf(candidate) == lastMillis && lastTimestamp.compareAndSet(last, candidate))
                    return candidate;
            }
        }
    }

    // Package visible for testing
    static long fromUnixTimestamp(long tstamp) {
        return (tstamp - START_EPOCH) * 10000;
    }

    private static long millisOf(long timestamp) {
        return timestamp / 10000;
    }

    // Package visible for testing
    static long makeMSB(long timestamp) {
        long msb = 0L;
        msb |= (0x00000000ffffffffL & timestamp) << 32;
        msb |= (0x0000ffff00000000L & timestamp) >>> 16;
        msb |= (0x0fff000000000000L & timestamp) >>> 48;
        msb |= 0x0000000000001000L; // sets the version to 1.
        return msb;
    }

    private static Set<String> getAllLocalAddresses() {
        Set<String> allIps = new HashSet<String>();
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            allIps.add(localhost.toString());
            // Also return the hostname if available, it won't hurt (this does a dns lookup, it's only done once at startup)
            allIps.add(localhost.getCanonicalHostName());
            InetAddress[] allMyIps = InetAddress.getAllByName(localhost.getCanonicalHostName());
            if (allMyIps != null) {
                for (int i = 0; i < allMyIps.length; i++)
                    allIps.add(allMyIps[i].toString());
            }
        } catch (UnknownHostException e) {
            // Ignore, we'll try the network interfaces anyway
        }

        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            if (en != null) {
                while (en.hasMoreElements()) {
                    Enumeration<InetAddress> enumIpAddr = en.nextElement().getInetAddresses();
                    while (enumIpAddr.hasMoreElements())
                        allIps.add(enumIpAddr.nextElement().toString());
                }
            }
        } catch (SocketException e) {
            // Ignore, if we've really got nothing so far, we'll throw an exception
        }

        return allIps;
    }
}
