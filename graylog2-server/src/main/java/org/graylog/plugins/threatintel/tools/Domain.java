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
package org.graylog.plugins.threatintel.tools;

public class Domain {

    public static String prepareDomain(final String domain) {
        // A typical issue is regular expressions that also capture a whitespace at the beginning or the end.
        String trimmedDomain = domain.trim();

        // Some systems will capture DNS requests with a trailing '.'. Remove that for the lookup.
        if(trimmedDomain.endsWith(".")) {
            trimmedDomain = trimmedDomain.substring(0, trimmedDomain.length()-1);
        }

        return trimmedDomain;
    }

}
