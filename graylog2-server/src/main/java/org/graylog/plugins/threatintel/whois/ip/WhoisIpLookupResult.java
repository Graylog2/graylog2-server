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
package org.graylog.plugins.threatintel.whois.ip;

import com.google.common.base.Strings;
import com.google.common.collect.ForwardingMap;

import java.util.HashMap;
import java.util.Map;

public class WhoisIpLookupResult extends ForwardingMap<String, Object> {

    private static final String NA = "N/A";

    private static WhoisIpLookupResult EMPTY = new WhoisIpLookupResult(NA, NA);

    private final String organization;
    private final String countryCode;

    private String prefix;

    WhoisIpLookupResult(String organization, String countryCode) {
        this.organization = organization;
        this.countryCode = countryCode;
    }

    static WhoisIpLookupResult empty() {
        return EMPTY;
    }

    public String getOrganization() {
        if (Strings.isNullOrEmpty(organization)) {
            return NA;
        } else {
            return organization;
        }
    }

    public String getCountryCode() {
        if (Strings.isNullOrEmpty(countryCode)) {
            return NA;
        } else {
            return countryCode;
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, Object> getResults() {
        final StringBuilder keyOrg = new StringBuilder();
        final StringBuilder keyCountryCode = new StringBuilder();

        if (prefix != null && !prefix.isEmpty()) {
            keyOrg.append(prefix).append("_");
            keyCountryCode.append(prefix).append("_");
        }

        keyOrg.append("whois_organization");
        keyCountryCode.append("whois_country_code");

        final var results = new HashMap<String, Object>();
        results.put(keyOrg.toString(), getOrganization());
        results.put(keyCountryCode.toString(), getCountryCode());
        return results;
    }

    @Override
    protected Map<String, Object> delegate() {
        return getResults();
    }

}
