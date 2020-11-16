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
package org.graylog.storage.elasticsearch6.views.searchtypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.core.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class MockSearchResult extends SearchResult {
    private final List<Map<String, Object>> hits;
    private final Long total;

    MockSearchResult(List<Map<String, Object>> hits, Long total) {
        super((ObjectMapper)null);
        this.hits = hits;
        this.total = total;
    }

    @Override
    public Long getTotal() {
        return this.total;
    }

    @Override
    public <T> List<Hit<T, Void>> getHits(Class<T> sourceType, boolean addEsMetadataFields) {
        final List<Hit<T, Void>> results = new ArrayList<>(this.hits.size());
        this.hits.forEach(hit -> results.add(new Hit<T, Void>((T)this.hits)));
        return results;
    }
}
