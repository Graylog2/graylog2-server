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
package org.graylog2.search;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Thrown by the {@link LuceneToMongoQueryTranslator} for queries that cannot be translated.
 */
public class InvalidLuceneQueryException extends IllegalArgumentException {
    public enum Reason {
        PARSE_ERROR,
        UNKNOWN_FIELD,
        INVALID_VALUE,
        UNSUPPORTED_QUERY
    }

    /**
     * The position of the invalid part of the query. Lines start at 1, columns at 0, the end column is exclusive.
     */
    public record Position(int beginLine, int beginColumn, int endLine, int endColumn) {}

    private final Reason reason;
    @Nullable
    private final Position position;

    public InvalidLuceneQueryException(Reason reason, String message) {
        this(reason, message, null, null);
    }

    public InvalidLuceneQueryException(Reason reason, String message, @Nullable Position position, @Nullable Throwable cause) {
        super(message, cause);
        this.reason = reason;
        this.position = position;
    }

    public Reason reason() {
        return reason;
    }

    public Optional<Position> position() {
        return Optional.ofNullable(position);
    }
}
