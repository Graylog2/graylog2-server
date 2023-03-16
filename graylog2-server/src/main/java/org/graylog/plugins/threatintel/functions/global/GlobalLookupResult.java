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
package org.graylog.plugins.threatintel.functions.global;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

public class GlobalLookupResult extends ForwardingMap<String, Object> {

    public static final String RESULTS_KEY = "threat_indicated";

    private final ImmutableMap<String, Object> results;

    private GlobalLookupResult(ImmutableMap<String, Object> fields) {
        this.results = fields;
    }

    static GlobalLookupResult fromMatches(List<String> matches, String prefix) {
        ImmutableMap.Builder<String, Object> fields = new ImmutableMap.Builder<>();

        // False matrch
        if(matches.isEmpty()) {
            fields.put(prefixedField(prefix, RESULTS_KEY), false);
            return new GlobalLookupResult(fields.build());
        }

        fields.put(prefixedField(prefix, RESULTS_KEY), true);

        for (String match : matches) {
            // threat_indicated_spamhaus => true
            fields.put(prefixedField(prefix, RESULTS_KEY) + "_" + match, true);
        }

        return new GlobalLookupResult(fields.build());
    }

    public Map<String, Object> getResults() {
        return results;
    }

    private static String prefixedField(String prefix, String field) {
        return prefix + "_" + field;
    }

    @Override
    protected Map<String, Object> delegate() {
        return getResults();
    }

}