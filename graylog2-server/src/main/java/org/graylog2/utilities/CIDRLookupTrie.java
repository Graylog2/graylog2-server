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

import java.util.Locale;

/**
 * Binary tree used to for efficient lookups in CIDR data adapters.
 */
public class CIDRLookupTrie {
    private static class TrieNode {
        // Binary tree node
        TrieNode[] children = new TrieNode[2];
        // The lookup value of the range, also our indicator that a node represents the end of a range.
        String rangeName = null;
        // Whether the range is an IPv4 or IPv6 CIDR range, only matters for nodes representing the end of a range.
        boolean rangeIsIPv6 = false;
    }

    private final TrieNode root = new TrieNode();

    /**
     * Insert a CIDR range into the trie
     *
     * @param cidr properly formatted CIDR address (must include '/rangePrefix' even if it is a single address
     * @param rangeName the name of the CIDR range
     */
    public void insertCIDR(String cidr, String rangeName) {
        final String[] parts = cidr.split("/");
        final String ip = parts[0];
        final int prefixLength = Integer.parseInt(parts[1]);

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
    }

    /**
     * Returns the rangeName of the range with the longest prefix that contains the IP address or null if one does not
     * exist.
     *
     * @param ip IP address to check against the collection of ranges
     * @return the name of the range with the longest prefix that contains the IP if it exists, null otherwise
     */
    public String longestPrefixRangeLookup(String ip) {
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

            if (currentNode.rangeName != null && (currentNode.rangeIsIPv6 == ipIsIPv6)) {
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

    // Convert an IP address to a binary string (supports both IPv4 and IPv6)
    private String toBinaryString(String ip, int prefixLength) {
        if (ip.contains(".")) {
            // IPv4
            StringBuilder binary = new StringBuilder();
            String[] octets = ip.split("\\.");
            for (String octet : octets) {
                String binaryOctet = String.format(Locale.ROOT, "%8s", Integer.toBinaryString(Integer.parseInt(octet))).replace(' ', '0');
                binary.append(binaryOctet);
            }
            return binary.toString();
        } else if (ip.contains(":")) {
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
        } else {
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
