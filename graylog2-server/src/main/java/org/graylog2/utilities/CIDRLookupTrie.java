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

import java.util.Locale;

/**
 * Binary tree used to for efficient lookups in CIDR data adapters.
 * NOTE: This class is NOT thread-safe. Use {@link #copy()} to clone the trie, make modifications, and then atomically
 * replace the in-use copy if needed.
 */
public class CIDRLookupTrie {
    private static class TrieNode {
        // Binary tree node
        TrieNode[] children = new TrieNode[2];
        // The lookup value of the range, also our indicator that a node represents the end of a range.
        String rangeName = null;
        // Whether the range is an IPv4 or IPv6 CIDR range, only matters for nodes representing the end of a range.
        boolean rangeIsIPv6 = false;
        // Time in millis after which the node is considered expired
        long expireAfter = 0L;

        TrieNode deepCopy() {
            TrieNode newNode = new TrieNode();
            newNode.rangeName = this.rangeName;
            newNode.rangeIsIPv6 = this.rangeIsIPv6;
            newNode.expireAfter = this.expireAfter;
            if (this.children[0] != null) {
                newNode.children[0] = this.children[0].deepCopy();
            }
            if (this.children[1] != null) {
                newNode.children[1] = this.children[1].deepCopy();
            }

            return newNode;
        }
    }

    private TrieNode root = new TrieNode();

    public CIDRLookupTrie copy() {
        final CIDRLookupTrie copy = new CIDRLookupTrie();
        copy.root = this.root.deepCopy();
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
        TrieNode currentNode = root;

        for (int i = 0; i < prefixLength; i++) {
            int bit = binaryIP.charAt(i) - '0';
            if (currentNode.children[bit] == null) {
                currentNode.children[bit] = new TrieNode();
            }
            currentNode = currentNode.children[bit];
        }

        currentNode.rangeName = rangeName;
        currentNode.rangeIsIPv6 = ip.contains(":");
        currentNode.expireAfter = expireAfter;
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
        final String binaryIP = toBinaryString(ip, -1);
        final boolean ipIsIPv6 = ip.contains(":");
        TrieNode currentNode = root;
        String longestMatchRangeName = null;

        for (int i = 0; i < binaryIP.length(); i++) {
            int bit = binaryIP.charAt(i) - '0';

            if (currentNode.children[bit] == null) {
                break;
            }

            currentNode = currentNode.children[bit];

            // Current node has a range name, is the same protocol, and TTL is either not a concern or is valid.
            if (currentNode.rangeName != null && currentNode.rangeIsIPv6 == ipIsIPv6
                    && (lookupTimeMillis == 0L || currentNode.expireAfter == 0 || lookupTimeMillis <= currentNode.expireAfter)) {
                longestMatchRangeName = currentNode.rangeName;
            }
        }

        return longestMatchRangeName;
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
        remove(root, binaryIP, 0, prefixLength);
    }

    public void cleanupExpiredNodes() {
        cleanupNode(root, DateTime.now(DateTimeZone.UTC).getMillis());
    }

    private boolean cleanupNode(TrieNode node, long now) {
        if (node == null) {
            return true;
        }

        // Determine if this node has expired
        boolean nodeExpired = node.expireAfter > 0L && node.expireAfter < now;
        // Recursively cleanup child nodes
        boolean leftExpired = cleanupNode(node.children[0], now);
        boolean rightExpired = cleanupNode(node.children[1], now);

        // If both child nodes and this node are expired, return true
        if (nodeExpired && leftExpired && rightExpired) {
            return true;
        }

        // Cleanup expired child nodes
        if (leftExpired) node.children[0] = null;
        if (rightExpired) node.children[1] = null;

        return false;
    }

    // Convert an IP address to a binary string (supports both IPv4 and IPv6)
    private String toBinaryString(String ip, int prefixLength) {
        final boolean isIPv6 = ip.contains(":");
        try {
            if (!isIPv6) {
                // IPv4
                StringBuilder binary = new StringBuilder();
                String[] octets = ip.split("\\.");
                for (String octet : octets) {
                    String binaryOctet = String.format(Locale.ROOT, "%8s", Integer.toBinaryString(Integer.parseInt(octet))).replace(' ', '0');
                    binary.append(binaryOctet);
                }
                return binary.toString();
            } else {
                // IPv6
                StringBuilder binary = new StringBuilder();
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
                }
                return binary.toString();
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid IP address format: " + ip);
        }
    }

    // Recursive call to remove a CIDR range from the trie as well as any empty nodes after removal.
    private boolean remove(TrieNode node, String binaryIP, int depth, int prefixLength) {
        if (node == null) {
            return false;
        }

        // Base case: we've reached the end of the prefix
        if (depth == prefixLength) {
            node.rangeName = null;

            // Check if the node can be deleted (no children)
            return node.children[0] == null && node.children[1] == null;
        }

        int bit = binaryIP.charAt(depth) - '0';
        boolean shouldDeleteChild = remove(node.children[bit], binaryIP, depth + 1, prefixLength);

        if (shouldDeleteChild) {
            // Remove the child node
            node.children[bit] = null;

            // Check if this node can also be deleted
            return node.rangeName == null && node.children[0] == null && node.children[1] == null;
        }

        return false;
    }


}
