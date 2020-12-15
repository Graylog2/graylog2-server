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
package org.graylog2.indexer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class QueryParsingException extends ElasticsearchException {
    private final Integer line;
    private final Integer column;
    private final String index;

    public QueryParsingException(String message, Integer line, Integer column, String index) {
        this(message, line, column, index, Collections.emptyList());
    }

    public QueryParsingException(String message, Integer line, Integer column, String index, List<String> errorDetails) {
        super(message, errorDetails);
        this.line = line;
        this.column = column;
        this.index = index;
    }

    public Optional<Integer> getLine() {
        return Optional.ofNullable(line);
    }

    public Optional<Integer> getColumn() {
        return Optional.ofNullable(column);
    }

    public Optional<String> getIndex() {
        return Optional.ofNullable(index);
    }
}
