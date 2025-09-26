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
import com.fasterxml.jackson.annotation.JsonInclude;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.Base64;
import static java.nio.charset.StandardCharsets.UTF_8;

// TODO pagination is really awkward to apply to our internal paginated services at the moment.
// as there seems to be little support for pagination in general in MCP clients, we'll skip it for now.
@JsonInclude(NON_NULL)
public record PaginatedList<T>(@Nonnull List<T> list, @Nullable Cursor cursor) {
    public PaginatedList {
        list = List.copyOf(list);
        if (cursor != null && (cursor.nextCursor == null || cursor.nextCursor.isBlank())) {
            throw new IllegalArgumentException("cursor.nextCursor must be non-blank if cursor is provided");
        }
    }

    public boolean hasNext() {
        return cursor != null && cursor.nextCursor != null && !cursor.nextCursor.isBlank();
    }

    public Optional<String> nextCursor() {
        return Optional.ofNullable(cursor).map(Cursor::nextCursor).filter(s -> !s.isBlank());
    }

    public <R> PaginatedList<? extends R> map(@Nonnull Function<? super T, ? extends R> mapper) {
        List<? extends R> mapped = list.stream().map(mapper).toList();
        return new PaginatedList<>(mapped, cursor);
    }

//    public <R> PaginatedList<? extends List<? extends R>> mapCompact(@Nonnull Function<? super T, List<? extends R>> flattener) {
//        List<? extends List<? extends R>> mapped = list.stream()
//                .map(flattener::apply)
//                .toList();
//        return new PaginatedList<>(mapped, cursor);
//    }

    public static <T> PaginatedList<T> empty() {
        return new PaginatedList<>(List.of(), null);
    }

    public record Cursor(String nextCursor) {
    }

    public static String encodeCursor(@Nonnull String grn) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(grn.getBytes(UTF_8));
    }

    public static String decodeCursor(String cursor) {
        return cursor == null ? null : new String(Base64.getUrlDecoder().decode(cursor), UTF_8);
    }
}
