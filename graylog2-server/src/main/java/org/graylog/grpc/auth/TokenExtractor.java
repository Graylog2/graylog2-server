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
package org.graylog.grpc.auth;

import io.grpc.Metadata;

import java.util.Optional;

@FunctionalInterface
public interface TokenExtractor {
    /**
     * Extracts a token from metadata, e.g. a custom header or the more standardized "Authorization: Bearer..."
     *
     * @param metadata to extract the token from
     * @return The extracted string
     * @throws IllegalArgumentException if token extraction fails. The message of the exception will be included in
     *                                  the response to the server, so be careful not to leak sensitive details.
     */
    Optional<String> extract(Metadata metadata) throws IllegalArgumentException;
}
