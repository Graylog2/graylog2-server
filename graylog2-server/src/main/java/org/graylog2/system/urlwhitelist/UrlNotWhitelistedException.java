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
package org.graylog2.system.urlwhitelist;

/**
 * Indicates that there was an attempt to access a URL which is not whitelisted.
 */
public class UrlNotWhitelistedException extends Exception {

    /**
     * Create an exception with a message stating that the given URL is not whitelisted.
     */
    public static UrlNotWhitelistedException forUrl(String url) {
        return new UrlNotWhitelistedException("URL <" + url + "> is not whitelisted.");
    }

    public UrlNotWhitelistedException(String message) {
        super(message);
    }
}
