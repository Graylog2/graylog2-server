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
package org.graylog.mcp.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;

import java.util.List;

// TODO pagination is really awkward to apply to our internal paginated services at the moment.
// as there seems to be little support for pagination in general in MCP clients, we'll skip it for now.
public record PaginatedList<T>(@Nonnull List<T> list, @Nullable Cursor cursor) {
    public record Cursor(String nextCursor) {}
}
