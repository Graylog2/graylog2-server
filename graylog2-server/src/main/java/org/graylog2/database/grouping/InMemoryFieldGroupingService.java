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
import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
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

        // Build MongoDB query filter
        final Bson queryFilter = !Strings.isNullOrEmpty(query)
                ? Filters.regex(fieldName, query, "i")
                : Filters.empty();

        // Fetch all matching documents
        final FindIterable<Document> documents = mongoCollection.find(queryFilter);

        // Create permission check predicate
        final Predicate<Document> permissionCheck = permissionsUtils.createPermissionCheck(subject, collectionName);

        // Filter documents by permissions and group by field
        final Map<String, Long> groupCounts = StreamSupport.stream(documents.spliterator(), false)
                .filter(permissionCheck)
                .filter(doc -> doc.containsKey(fieldName) && doc.get(fieldName) != null)
                .collect(Collectors.groupingBy(
                        doc -> doc.get(fieldName).toString(),
                        Collectors.counting()
                ));

        // Apply buckets filter
        final Pattern bucketsPattern = !Strings.isNullOrEmpty(bucketsFilter)
                ? Pattern.compile(bucketsFilter, Pattern.CASE_INSENSITIVE)
                : null;

        final List<EntityFieldBucket> allBuckets = groupCounts.entrySet().stream()
                .filter(entry -> bucketsPattern == null || bucketsPattern.matcher(entry.getKey()).find())
                .map(entry -> new EntityFieldBucket(entry.getKey(), entry.getKey(), entry.getValue()))
                .toList();

        // Sort buckets
        final Comparator<EntityFieldBucket> comparator = buildComparator(sortOrder, sortField);
        final List<EntityFieldBucket> sortedBuckets = allBuckets.stream()
                .sorted(comparator)
                .toList();

        // Apply pagination
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
