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
package org.graylog.plugins.threatintel.whois.ip.parsers;

public class ARINResponseParser extends WhoisParser {

    private NetworkType prevNetworkType = null;
    private NetworkType currNetworkType = null;

    @Override
    public void readLine(String line) {
        if (line.startsWith("#") || line.isEmpty()) {
            return;
        }

        // In some cases, ARIN may have multiple results with different NetType values.  When that happens,
        //  we want to use the data from the entry with the data closest to the customer actually using the IP.
        if (line.startsWith("NetType:")) {
            prevNetworkType = currNetworkType;
            currNetworkType = NetworkType.getEnum(lineValue(line));
            if (null != currNetworkType && currNetworkType.isMoreSpecificThan(prevNetworkType)) {
                this.organization = null;
                this.countryCode = null;
            }
        }

        if((line.startsWith("Organization:") || line.startsWith("Customer:")) && this.organization == null) {
            this.organization = lineValue(line);
        }

        if(line.startsWith("Country:") && this.countryCode == null) {
            this.countryCode = lineValue(line);
        }

        if(line.startsWith("ResourceLink") && !line.contains("http")) {
            this.isRedirect = true;
            registryRedirect = findRegistryFromWhoisServer(lineValue(line));
        }
    }

    @Override
    public String buildQueryForIp(String ip) {
        // This query ensures that we get all of the records when there are multiple results rather than just a list of
        //  record summaries without details.
        return "n + " + ip;
    }

    private enum NetworkType {
        // Network types are defined in ARIN's documentation: https://www.arin.net/resources/registry/whois/#network
        // Arranged in order of decreasing preference.  Do not reorder unless preference order changes
        REASSIGNED("Reassigned"),
        DIRECT_ASSIGNMENT("Direct Assignment"),
        REALLOCATED("Reallocated"),
        DIRECT_ALLOCATION("Direct Allocation");

        private String displayName;

        NetworkType(String displayName) { this.displayName = displayName; }

        String displayName() { return displayName; }

        boolean isMoreSpecificThan(NetworkType netType) {
            if (null == netType) {
                return true;
            }
            return (this.ordinal() < netType.ordinal());
        }

        static NetworkType getEnum(String value) {
            for (NetworkType v : values()) {
                if (value.equalsIgnoreCase(v.displayName())) {
                    return v;
                }
            }
            return null;
        }
    }
}
