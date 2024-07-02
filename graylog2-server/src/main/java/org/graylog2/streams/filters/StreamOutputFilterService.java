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
package org.graylog2.streams.filters;

import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;

import java.util.Optional;
import java.util.function.Predicate;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedId;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;
import static org.graylog2.streams.filters.StreamOutputFilterRuleDTO.FIELD_DESCRIPTION;
import static org.graylog2.streams.filters.StreamOutputFilterRuleDTO.FIELD_OUTPUT_TARGET;
import static org.graylog2.streams.filters.StreamOutputFilterRuleDTO.FIELD_STATUS;
import static org.graylog2.streams.filters.StreamOutputFilterRuleDTO.FIELD_STREAM_ID;
import static org.graylog2.streams.filters.StreamOutputFilterRuleDTO.FIELD_TITLE;

public class StreamOutputFilterService {
    static final String COLLECTION = "stream_output_filters";

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(FIELD_TITLE, SearchQueryField.create(FIELD_TITLE))
            .put(FIELD_DESCRIPTION, SearchQueryField.create(FIELD_DESCRIPTION))
            .put(FIELD_OUTPUT_TARGET, SearchQueryField.create(FIELD_OUTPUT_TARGET))
            .put(FIELD_STATUS, SearchQueryField.create(FIELD_STATUS))
            .build();

    private final MongoCollection<StreamOutputFilterRuleDTO> collection;
    private final MongoPaginationHelper<StreamOutputFilterRuleDTO> paginationHelper;
    private final MongoUtils<StreamOutputFilterRuleDTO> utils;

    @Inject
    public StreamOutputFilterService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION, StreamOutputFilterRuleDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.utils = mongoCollections.utils(collection);
    }

    private Bson parseQuery(String queryString) {
        final var queryParser = new SearchQueryParser(FIELD_TITLE, SEARCH_FIELD_MAPPING);
        return queryParser.parse(queryString).toBson();
    }

    public PaginatedList<StreamOutputFilterRuleDTO> findPaginatedForStream(
            String streamId,
            String queryString,
            Bson sort,
            int perPage,
            int page,
            Predicate<String> permissionSelector
    ) {
        final var query = parseQuery(queryString);

        return paginationHelper.filter(and(eq(FIELD_STREAM_ID, streamId), query))
                .sort(sort)
                .perPage(perPage)
                .page(page, dto -> permissionSelector.test(dto.id()));
    }

    public PaginatedList<StreamOutputFilterRuleDTO> findPaginatedForStreamAndTarget(
            String streamId,
            String targetId,
            String queryString,
            Bson sort,
            int perPage,
            int page,
            Predicate<String> permissionSelector
    ) {
        final var query = parseQuery(queryString);

        return paginationHelper.filter(and(eq(FIELD_STREAM_ID, streamId), eq(FIELD_OUTPUT_TARGET, targetId), query))
                .sort(sort)
                .perPage(perPage)
                .page(page, dto -> permissionSelector.test(dto.id()));
    }

    public Optional<StreamOutputFilterRuleDTO> findById(String id) {
        return utils.getById(id);
    }

    public StreamOutputFilterRuleDTO create(StreamOutputFilterRuleDTO dto) {
        if (!isBlank(dto.id())) {
            throw new IllegalArgumentException("id must be blank");
        }

        return utils.getById(insertedId(collection.insertOne(dto)))
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't insert document: %s", dto)));
    }

    public StreamOutputFilterRuleDTO update(StreamOutputFilterRuleDTO dto) {
        collection.replaceOne(idEq(requireNonBlank(dto.id(), "id can't be blank")), dto);

        return utils.getById(dto.id())
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't find updated document: %s", dto)));
    }

    public StreamOutputFilterRuleDTO delete(String id) {
        final var dto = utils.getById(id)
                .orElseThrow(() -> new IllegalArgumentException(f("Couldn't find document with ID <%s> for deletion", id)));

        collection.deleteOne(idEq(id));

        return dto;
    }
}
