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
package org.graylog2.database.grouping;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Sorts;
import org.apache.shiro.subject.Subject;
import org.bson.conversions.Bson;

import java.util.List;

public interface EntityFieldGroupingService {

    String COUNT_FIELD_NAME = "count";
    String ID_FIELD_NAME = "_id";
    List<String> SORT_BY_COUNT_FIELDS = List.of(COUNT_FIELD_NAME, ID_FIELD_NAME);
    List<String> SORT_BY_VALUE_FIELDS = SORT_BY_COUNT_FIELDS.reversed();

    EntityFieldBucketResponse groupByField(String collectionName,
                                           String fieldName,
                                           String query,
                                           String bucketsFilter,
                                           int page,
                                           int pageSize,
                                           SortOrder sortOrder,
                                           SortField sortField,
                                           Subject subject);

    default Bson buildSortStage(final SortOrder sortOrder,
                                final SortField sortField) {
        final List<String> sort = switch (sortField) {
            case COUNT -> SORT_BY_COUNT_FIELDS;
            case VALUE -> SORT_BY_VALUE_FIELDS;
        };
        return switch (sortOrder) {
            case ASC -> Aggregates.sort(Sorts.ascending(sort));
            case DESC -> Aggregates.sort(Sorts.descending(sort));
        };
    }

    enum SortOrder {
        ASC,
        DESC
    }

    enum SortField {
        COUNT,
        VALUE
    }
}
