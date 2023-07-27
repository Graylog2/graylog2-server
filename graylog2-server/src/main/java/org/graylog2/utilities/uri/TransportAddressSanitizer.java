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
package org.graylog2.utilities.uri;

import org.apache.http.client.utils.URIBuilder;

import javax.inject.Singleton;
import java.net.URI;
import java.net.URISyntaxException;

@Singleton
public class TransportAddressSanitizer {

    static final String INVALID_URI = "Invalid URI";

    public String withRemovedCredentials(final String orig) {
        try {
            final URI uri = new URI(orig);
            if (uri.getUserInfo() != null) {
                return new URIBuilder(uri)
                        .setUserInfo(null)
                        .toString();
            } else {
                return orig;
            }
        } catch (URISyntaxException ex) {
            return INVALID_URI;
        }
    }
}
