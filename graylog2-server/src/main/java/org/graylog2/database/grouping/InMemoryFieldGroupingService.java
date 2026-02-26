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
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InMemoryFieldGroupingService implements EntityFieldGroupingService {

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public InMemoryFieldGroupingService(final MongoConnection mongoConnection,
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

        final FindIterable<Document> documents = mongoCollection
                .find(buildQueryFilterBson(fieldName, query))
                .projection(Projections.include(fieldName));

        final Map<String, Long> groupCounts = StreamSupport.stream(documents.spliterator(), false)
                .filter(permissionsUtils.createPermissionCheck(subject, collectionName))
                .collect(Collectors.groupingBy(
                        doc -> {
                            if (docHasNoValueForField(fieldName, doc)) {
                                return "";
                            }
                            return doc.get(fieldName).toString();
                        },
                        Collectors.counting()
                ));

        final Pattern bucketsPattern = !Strings.isNullOrEmpty(bucketsFilter)
                ? Pattern.compile(bucketsFilter, Pattern.CASE_INSENSITIVE)
                : null;

        final List<EntityFieldBucket> sortedBuckets = groupCounts.entrySet().stream()
                .filter(entry -> bucketsPattern == null || bucketsPattern.matcher(entry.getKey()).find())
                .map(entry -> new EntityFieldBucket(entry.getKey(), entry.getKey(), entry.getValue()))
                .sorted(buildComparator(sortOrder, sortField))
                .toList();

        final int total = sortedBuckets.size();
        final int skip = (page - 1) * pageSize;
        final List<EntityFieldBucket> paginatedBuckets = sortedBuckets.stream()
                .skip(skip)
                .limit(pageSize)
                .toList();

        return new EntityFieldBucketResponse(
                paginatedBuckets,
                PaginatedList.PaginationInfo.create(total, paginatedBuckets.size(), page, pageSize)
        );
    }

    private boolean docHasNoValueForField(final String fieldName, final Document doc) {
        return !doc.containsKey(fieldName) || doc.get(fieldName) == null;
    }

    private Comparator<EntityFieldBucket> buildComparator(final SortOrder sortOrder, final SortField sortField) {
        final Comparator<EntityFieldBucket> baseComparator = switch (sortField) {
            case COUNT -> Comparator.comparingLong(EntityFieldBucket::count)
                    .thenComparing(EntityFieldBucket::fieldValue);
            case VALUE -> Comparator.comparing(EntityFieldBucket::fieldValue)
                    .thenComparingLong(EntityFieldBucket::count);
        };

        return switch (sortOrder) {
            case ASC -> baseComparator;
            case DESC -> baseComparator.reversed();
        };
    }
}
