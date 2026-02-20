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

import com.google.common.base.Strings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.StreamSupport;

public class MongoEntityFieldGroupingService implements EntityFieldGroupingService {

    private static final Logger LOG = LoggerFactory.getLogger(MongoEntityFieldGroupingService.class);

    static final String COUNT_FIELD_NAME = "count";
    static final String ID_FIELD_NAME = "_id";
    static final List<String> SORT_BY_COUNT_FIELDS = List.of(COUNT_FIELD_NAME, ID_FIELD_NAME);
    static final List<String> SORT_BY_VALUE_FIELDS = SORT_BY_COUNT_FIELDS.reversed();

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public MongoEntityFieldGroupingService(final MongoConnection mongoConnection,
                                           final EntityPermissionsUtils permissionsUtils) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
    }

    @Override
    public EntityFieldBucketResponse groupByField(final String collectionName,
                                                  final String fieldName,
                                                  final String query,
                                                  final String bucketsFilter,
                                                  final int page,
                                                  final int pageSize,
                                                  final SortOrder sortOrder,
                                                  final SortField sortField,
                                                  final Subject subject) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collectionName);
        final var userCanReadAllEntities = permissionsUtils.hasAllPermission(subject) ||
                permissionsUtils.hasReadPermissionForWholeCollection(subject, collectionName);

        if (userCanReadAllEntities) {
            final var queryFilterBson = !Strings.isNullOrEmpty(query)
                    ? Filters.regex(fieldName, query, "i")
                    : Filters.empty();

            final var bucketsFilterBson = !Strings.isNullOrEmpty(bucketsFilter)
                    ? Filters.regex(ID_FIELD_NAME, bucketsFilter, "i")
                    : Filters.empty();
            
            final int total = getTotalNumberOfBuckets(fieldName, mongoCollection, queryFilterBson, bucketsFilterBson);
            final List<EntityFieldBucket> paginatedAndSortedResults = getPage(fieldName, page, pageSize, sortOrder, sortField, mongoCollection, queryFilterBson, bucketsFilterBson);

            return new EntityFieldBucketResponse(paginatedAndSortedResults,
                    PaginatedList.PaginationInfo.create(
                            total,
                            paginatedAndSortedResults.size(),
                            page,
                            pageSize)
            );
        } else {
            LOG.warn("Returning empty results for slice-by - user does not have permission to read all entities in collection '{}'", collectionName);
            //TODO: undoable in MongoDB, only possible in-memory, but highly inefficient
            return new EntityFieldBucketResponse(List.of(), PaginatedList.PaginationInfo.create(0, 0, page, pageSize));
        }
    }

    private List<EntityFieldBucket> getPage(final String fieldName,
                                            final int page,
                                            final int pageSize,
                                            final SortOrder sortOrder,
                                            final SortField sortField,
                                            final MongoCollection<Document> mongoCollection,
                                            final Bson queryFilter,
                                            final Bson bucketsFilter) {
        final AggregateIterable<Document> aggregateIterable = mongoCollection.aggregate(
                List.of(
                        Aggregates.match(queryFilter),
                        Aggregates.group("$" + fieldName, Accumulators.sum(COUNT_FIELD_NAME, 1)),
                        Aggregates.match(bucketsFilter),
                        buildSortStage(sortOrder, sortField),
                        Aggregates.skip((page - 1) * pageSize),
                        Aggregates.limit(pageSize)
                )
        );

        return StreamSupport.stream(aggregateIterable.spliterator(), false)
                .map(doc -> {
                    final String value = doc.get(ID_FIELD_NAME).toString();
                    final String title = value;//it is very likely that BE should fetch related entities and enrich second field of EntityFieldGroup class, instead of using value there
                    return new EntityFieldBucket(value, title, doc.getInteger(COUNT_FIELD_NAME, 0));
                })
                .toList();
    }

    private int getTotalNumberOfBuckets(final String fieldName,
                                        final MongoCollection<Document> mongoCollection,
                                        final Bson queryFilter,
                                        final Bson bucketsFilter) {
        final Document totalCountResults = mongoCollection.aggregate(
                List.of(
                        Aggregates.match(queryFilter),
                        Aggregates.group("$" + fieldName),
                        Aggregates.match(bucketsFilter),
                        Aggregates.count("number_of_groups")
                )
        ).first();

        return totalCountResults != null ? totalCountResults.getInteger("number_of_groups", 0) : 0;
    }

    private Bson buildSortStage(final SortOrder sortOrder,
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
}
