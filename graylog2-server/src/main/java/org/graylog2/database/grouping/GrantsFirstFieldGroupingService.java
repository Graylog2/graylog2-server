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
import jakarta.inject.Inject;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.graylog2.shared.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GrantsFirstFieldGroupingService implements EntityFieldGroupingService {

    private static final Logger LOG = LoggerFactory.getLogger(GrantsFirstFieldGroupingService.class);

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;
    private final UserService userService;

    @Inject
    public GrantsFirstFieldGroupingService(final MongoConnection mongoConnection,
                                           final EntityPermissionsUtils permissionsUtils,
                                           final UserService userService) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
        this.userService = userService;
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
        // 1. Convert subject to user
        final String username = subject.getPrincipal().toString();
        final User user = userService.load(username);

        if (user == null) {
            LOG.warn("User {} not found, returning empty results", username);
            return new EntityFieldBucketResponse(List.of(), PaginatedList.PaginationInfo.create(0, 0, page, pageSize));
        }

        // 2. Get all user's permissions
        final List<WildcardPermission> permissions = userService.getWildcardPermissionsForUser(user);

        // 3. Find IDs of entities that belong to collectionName which user is allowed to see
        final Optional<String> readPermissionOpt = permissionsUtils.readPermissionForCollection(collectionName);

        if (readPermissionOpt.isEmpty()) {
            LOG.warn("No read permission defined for collection {}, returning empty results", collectionName);
            return new EntityFieldBucketResponse(List.of(), PaginatedList.PaginationInfo.create(0, 0, page, pageSize));
        }

        final String readPermission = readPermissionOpt.get();
        final Set<ObjectId> allowedEntityIds = extractAllowedEntityIds(permissions, readPermission);

        if (allowedEntityIds.isEmpty()) {
            LOG.debug("User {} has no permissions for collection {}, returning empty results", username, collectionName);
            return new EntityFieldBucketResponse(List.of(), PaginatedList.PaginationInfo.create(0, 0, page, pageSize));
        }

        // 4. Implement similar code to MongoEntityFieldGroupingService, but use the list of IDs as additional filter
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collectionName);

        final Bson queryFilter = buildQueryFilterBson(fieldName, query);
        final Bson idFilter = Filters.in("_id", allowedEntityIds);
        final Bson combinedFilter = Filters.and(queryFilter, idFilter);

        final Bson bucketsFilterBson = !Strings.isNullOrEmpty(bucketsFilter)
                ? Filters.regex(EntityFieldGroupingService.ID_FIELD_NAME, bucketsFilter, "i")
                : Filters.empty();

        final int total = getTotalNumberOfBuckets(fieldName, mongoCollection, combinedFilter, bucketsFilterBson);
        final List<EntityFieldBucket> paginatedAndSortedResults = getPage(fieldName, page, pageSize, sortOrder, sortField, mongoCollection, combinedFilter, bucketsFilterBson);

        return new EntityFieldBucketResponse(paginatedAndSortedResults,
                PaginatedList.PaginationInfo.create(
                        total,
                        paginatedAndSortedResults.size(),
                        page,
                        pageSize)
        );
    }

    private Set<ObjectId> extractAllowedEntityIds(final List<WildcardPermission> permissions, final String readPermission) {
        return permissions.stream()
                .map(PermissionWrapper::new)
                .filter(p -> {
                    final List<Set<String>> parts = p.getParts();
                    if (parts.size() != 3) {
                        return false;
                    }
                    // Check if permission matches readPermission format (e.g., "streams:read")
                    final String permissionString = String.join(":",
                            parts.get(0).iterator().next(),
                            parts.get(1).iterator().next());
                    return permissionString.equals(readPermission);
                })
                .map(p -> {
                    final List<Set<String>> parts = p.getParts();
                    final String entityId = parts.get(2).iterator().next();
                    return new ObjectId(entityId);
                })
                .collect(Collectors.toSet());
    }

    // Wrapper class to access protected getParts() method from WildcardPermission
    private static class PermissionWrapper extends WildcardPermission {
        public PermissionWrapper(WildcardPermission permission) {
            super(permission.toString());
        }

        @Override
        public List<Set<String>> getParts() {
            return super.getParts();
        }
    }

    private List<EntityFieldBucket> getPage(final String fieldName,
                                            final int page,
                                            final int pageSize,
                                            final SortOrder sortOrder,
                                            final SortField sortField,
                                            final MongoCollection<Document> mongoCollection,
                                            final Bson combinedFilter,
                                            final Bson bucketsFilter) {
        final AggregateIterable<Document> aggregateIterable = mongoCollection.aggregate(
                List.of(
                        Aggregates.match(combinedFilter),
                        Aggregates.group("$" + fieldName, Accumulators.sum(EntityFieldGroupingService.COUNT_FIELD_NAME, 1)),
                        Aggregates.match(bucketsFilter),
                        buildSortStage(sortOrder, sortField),
                        Aggregates.skip((page - 1) * pageSize),
                        Aggregates.limit(pageSize)
                )
        );

        return StreamSupport.stream(aggregateIterable.spliterator(), false)
                .map(doc -> {
                    final String value = doc.get(EntityFieldGroupingService.ID_FIELD_NAME) != null ? doc.get(EntityFieldGroupingService.ID_FIELD_NAME).toString() : "";
                    final String title = value;
                    return new EntityFieldBucket(value, title, doc.getInteger(EntityFieldGroupingService.COUNT_FIELD_NAME, 0));
                })
                .toList();
    }

    private int getTotalNumberOfBuckets(final String fieldName,
                                        final MongoCollection<Document> mongoCollection,
                                        final Bson combinedFilter,
                                        final Bson bucketsFilter) {
        final Document totalCountResults = mongoCollection.aggregate(
                List.of(
                        Aggregates.match(combinedFilter),
                        Aggregates.group("$" + fieldName),
                        Aggregates.match(bucketsFilter),
                        Aggregates.count("number_of_groups")
                )
        ).first();

        return totalCountResults != null ? totalCountResults.getInteger("number_of_groups", 0) : 0;
    }
}
