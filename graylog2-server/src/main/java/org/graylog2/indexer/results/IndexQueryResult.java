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
package org.graylog2.indexer.results;

public abstract class IndexQueryResult {
    private final String originalQuery;
    private final String builtQuery;

    public IndexQueryResult(final String originalQuery, final String builtQuery) {
        this.originalQuery = originalQuery;
        this.builtQuery = builtQuery;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public String getBuiltQuery() {
        return builtQuery;
    }

    public abstract long tookMs();
}
