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
package org.graylog.storage.opensearch3.views.export;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * This is a holder for searchAfter values during one export run. It's not thread safe and should be recreated
 * for each export.
 */
public class SearchAfterValues {

    private List<FieldValue> searchAfterValues;

    private SearchAfterValues() {
        this.searchAfterValues = null;
    }

    public static SearchAfterValues empty() {
        return new SearchAfterValues();
    }

    public <T> void update(SearchResponse<T> results) {
        searchAfterValues = lastHitSortFrom(results.hits().hits());
    }

    private <T> List<FieldValue> lastHitSortFrom(List<Hit<T>> hits) {
        return Optional.of(hits)
                .filter(h -> !h.isEmpty())
                .map(List::getLast)
                .map(Hit::sort)
                .orElse(null);
    }

    public void ifPresent(Consumer<List<FieldValue>> consumer) {
        if (searchAfterValues != null) {
            consumer.accept(searchAfterValues);
        }
    }
}
