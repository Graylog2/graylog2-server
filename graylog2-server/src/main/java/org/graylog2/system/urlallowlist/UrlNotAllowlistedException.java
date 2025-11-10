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
package org.graylog2.system.urlallowlist;

/**
 * Indicates that there was an attempt to access a URL which is not allowlisted.
 */
public class UrlNotAllowlistedException extends Exception {

    /**
     * Create an exception with a message stating that the given URL is not allowlisted.
     */
    public static UrlNotAllowlistedException forUrl(String url) {
        return new UrlNotAllowlistedException("URL <" + url + "> is not allowlisted.");
    }

    public UrlNotAllowlistedException(String message) {
        super(message);
    }
}
