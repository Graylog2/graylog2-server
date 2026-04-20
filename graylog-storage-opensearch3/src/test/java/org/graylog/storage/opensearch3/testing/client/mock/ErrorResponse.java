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
package org.graylog.storage.opensearch3.testing.client.mock;

import java.io.InputStream;
import java.nio.file.PathMatcher;

public record ErrorResponse(String method, PathMatcher urlPattern, int responseCode, String error) implements MockedResponse {

    @Override
    public InputStream newInputStream() {
        return InputStream.nullInputStream();
    }

    @Override
    public boolean isError() {
        return true;
    }
}
