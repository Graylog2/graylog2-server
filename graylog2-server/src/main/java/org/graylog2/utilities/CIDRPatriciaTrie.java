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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Locale;
import java.util.Map;

/**
 * PatriciaTrie used to for efficient lookups in CIDR data adapters.
 * NOTE: This class is NOT thread-safe. Use {@link #cleanCopy()} to clone the trie, make modifications, and then atomically
 * replace the in-use copy if needed.
 */
public class CIDRPatriciaTrie {
    @VisibleForTesting
    record Node(
            // The lookup value of the range
            String rangeName,
            // Whether the range is an IPv4 or IPv6 CIDR range
            boolean rangeIsIPv6,
            // Time in millis after which the node is considered expired
            long expireAfter) {
    }

    private PatriciaTrie<Node> trie = new PatriciaTrie<>();
    private int shortestV4Prefix = -1;
    private int shortestV6Prefix = -1;

    @VisibleForTesting
    boolean isEmpty() {
        return trie.isEmpty();
    }

    /**
     * Returns a deep copy of this CIDRPatriciaTrie with any expired nodes removed.
     *
     * @return deep copy of this trie
     */
    public CIDRPatriciaTrie cleanCopy() {
        final long now = DateTime.now(DateTimeZone.UTC).getMillis();
        final PatriciaTrie<Node> cleanTrie = new PatriciaTrie<>();
        int shortestV6 = -1;
        int shortestV4 = -1;
        for (Map.Entry<String, Node> entry : trie.entrySet()) {
            final Node data = entry.getValue();
            if (data.expireAfter == 0L || data.expireAfter > now) {
                final int prefixLength = entry.getKey().length();
                if (data.rangeIsIPv6 && (shortestV6 == -1 || prefixLength < shortestV6)) {
                    shortestV6 = prefixLength;
                } else if (!data.rangeIsIPv6 && (shortestV4 == -1 || prefixLength < shortestV4)) {
                    shortestV4 = prefixLength;
                }
                cleanTrie.put(entry.getKey(), new Node(data.rangeName, data.rangeIsIPv6, data.expireAfter));
            }
        }
        final CIDRPatriciaTrie copy = new CIDRPatriciaTrie();
        copy.trie = cleanTrie;
        copy.shortestV4Prefix = shortestV4;
        copy.shortestV6Prefix = shortestV6;
        return copy;
    }

    public void insertCIDR(String cidr, String rangeName) {
        insertCIDR(cidr, rangeName, 0L);
    }

    /**
     * Insert a CIDR range into the trie with a time-to-live
     *
     * @param cidr        properly formatted CIDR address (must include '/rangePrefix' even if it is a single address
     * @param rangeName   the name of the CIDR range
     * @param expireAfter epoch time in millis after which the CIDR should be expired
     */
    public void insertCIDR(String cidr, String rangeName, long expireAfter) {
        final String[] parts = cidr.split("/");
        final String ip = parts[0];
        final int prefixLength;
        try {
            prefixLength = Integer.parseInt(parts[1]);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unable to parse invalid CIDR range: " + cidr);
        }

        // Get binary representation of the IP
        final String binaryIP = toBinaryString(ip, prefixLength);
        final boolean isIPV6 = ip.contains(":");
        final Node node = new Node(rangeName, isIPV6, expireAfter);
        trie.put(binaryIP, node);
        if (isIPV6 && (shortestV6Prefix == -1 || prefixLength < shortestV6Prefix)) {
            this.shortestV6Prefix = prefixLength;
        } else if (!isIPV6 && (shortestV4Prefix == -1 || prefixLength < shortestV4Prefix)) {
            this.shortestV4Prefix = prefixLength;
        }
    }

    public String longestPrefixRangeLookup(String ip) {
        return longestPrefixRangeLookupWithTtl(ip, 0L);
    }

    /**
     * Returns the rangeName of the range with the longest prefix that contains the IP address or null if one does not
     * exist.
     *
     * @param ip               IP address to check against the collection of ranges
     * @param lookupTimeMillis time lookup was performed in epoch time milliseconds or 0 if node expiry is not a concern
     * @return the name of the range with the longest prefix that contains the IP if it exists, null otherwise
     */
    public String longestPrefixRangeLookupWithTtl(String ip, long lookupTimeMillis) {
        if (isEmpty()) {
            return null;
        }
        final String binaryIP = toBinaryString(ip, -1);
        final boolean lookupIsIPv6 = ip.contains(":");

        final int shortestPrefixForType = lookupIsIPv6 ? shortestV6Prefix : shortestV4Prefix;
        for (int i = binaryIP.length(); i >= shortestPrefixForType; i--) {
            final String lookupPrefix = binaryIP.substring(0, i);
            final Map<String, Node> prefixTrie = trie.prefixMap(lookupPrefix);
            for (Map.Entry<String, Node> entry : prefixTrie.entrySet()) {
                final Node rangeData = entry.getValue();
                final String binaryCidr = entry.getKey();
                if (lookupIsIPv6 == rangeData.rangeIsIPv6 &&
                        (rangeData.expireAfter == 0L || rangeData.expireAfter > lookupTimeMillis) &&
                        binaryIP.startsWith(binaryCidr)) {
                    return rangeData.rangeName;
                }
            }
        }

        return null;
    }

    /**
     * Remove a CIDR range from the trie and cleanup any empty nodes after removal.
     *
     * @param cidr range to remove
     */
    public void removeCIDR(String cidr) {
        final String[] parts = cidr.split("/");
        final String ip = parts[0];
        final int prefixLength = Integer.parseInt(parts[1]);
        final String binaryIP = toBinaryString(ip, prefixLength);
        final Node removedNode = trie.remove(binaryIP);
        if (removedNode != null) {
            final boolean isIPV6 = ip.contains(":");
            if ((isIPV6 && prefixLength == shortestV6Prefix) || (!isIPV6 && prefixLength == shortestV4Prefix)) {
                recalculateShortestPrefix(isIPV6);
            }
        }
    }

    public void recalculateShortestPrefix(boolean isIPV6) {
        if (trie.isEmpty()) {
            this.shortestV4Prefix = -1;
            this.shortestV6Prefix = -1;
        } else {
            int shortest = -1;
            long now = DateTime.now(DateTimeZone.UTC).getMillis();
            for (Map.Entry<String, Node> entry : trie.entrySet()) {
                final Node rangeData = entry.getValue();
                if (rangeData.rangeIsIPv6 == isIPV6 // same type of address
                        && (shortest == -1 || entry.getKey().length() < shortest) // shorter prefix than we've seen
                        && (rangeData.expireAfter == 0L || rangeData.expireAfter > now)) { // not expired
                    shortest = entry.getKey().length();
                }
            }
            if (isIPV6) {
                this.shortestV6Prefix = shortest;
            } else {
                this.shortestV4Prefix = shortest;
            }
        }
    }

    // Convert an IP address to a binary string (supports both IPv4 and IPv6)
    static String toBinaryString(String ip, int prefixLength) {
        final boolean isIPv6 = ip.contains(":");
        try {
            StringBuilder binary = new StringBuilder();
            if (!isIPv6) {
                // IPv4
                String[] octets = ip.split("\\.");
                for (String octet : octets) {
                    String binaryOctet = String.format(Locale.ROOT, "%8s", Integer.toBinaryString(Integer.parseInt(octet))).replace(' ', '0');
                    binary.append(binaryOctet);
                }
                return prefixLength > 0 ? binary.substring(0, prefixLength) : binary.toString();
            } else {
                // IPv6
                String[] hextets = ip.split(":");
                for (String hextet : hextets) {
                    if (!hextet.isEmpty()) {
                        String binaryHextet = String.format(Locale.ROOT, "%16s", Integer.toBinaryString(Integer.parseInt(hextet, 16))).replace(' ', '0');
                        binary.append(binaryHextet);
                    } else {
                        // Handle "::" shorthand for consecutive zero groups
                        int missingGroups = 8 - hextets.length + 1;
                        binary.append("0000000000000000".repeat(Math.max(0, missingGroups)));
                    }
                }
                // If the prefix length is larger than the resulting binary string, append 0 until the length matches. This
                // will avoid index out of range exceptions when inserting the range into the trie.
                if (binary.length() < prefixLength) {
                    binary.append("0".repeat(prefixLength - binary.length()));
                }
                // When getting binary string of individual IPv6 addresses, ensure binary string is complete 128 digits.
                if (prefixLength == -1) {
                    binary.append("0".repeat(Math.max(0, 128 - binary.length())));
                } else if (binary.length() > prefixLength) {
                    return binary.substring(0, prefixLength);
                }
                return binary.toString();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address format: " + ip);
        }
    }

    // Used only for testing purposes.
    @VisibleForTesting
    Node getNode(String key) {
        return trie.get(key);
    }
}
