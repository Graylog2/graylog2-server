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
package org.graylog2.database.aggregation;

import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog2.database.MongoCollections;

/**
 * Creates aggregators restricted to the documents a user may read, so that callers can't forget to restrict them.
 * <p>
 * The aggregated collection, and the related collection that a grouping looks up, if any, must be registered with
 * the {@link org.graylog2.database.DbEntity DbEntity} annotation. Their read permissions decide which documents the
 * user may read, see {@link CatalogPermissionFilters}. Aggregating a collection that isn't registered fails, instead of
 * reading all of it.
 * <p>
 * Collections whose read access the catalog can't express, like security events and their streams, are aggregated
 * with the permission filter their own code builds. Related collections are still filtered through the catalog.
 * <p>
 * Code that aggregates on behalf of the system rather than a user uses {@link MongoCollections#aggregator(String)}.
 */
public class PermittedAggregators {
    private final MongoCollections mongoCollections;
    private final CatalogPermissionFilters permissionFilters;

    @Inject
    public PermittedAggregators(final MongoCollections mongoCollections,
                                final CatalogPermissionFilters permissionFilters) {
        this.mongoCollections = mongoCollections;
        this.permissionFilters = permissionFilters;
    }

    /**
     * @param collection the collection to aggregate
     * @param subject    the user whose read permissions restrict the aggregated and related documents
     * @throws IllegalArgumentException if the collection isn't registered in the entities catalog. The aggregator
     *                                  throws it for a related collection that isn't registered.
     */
    public MongoAggregator forUser(final String collection, final Subject subject) {
        return forUser(collection, permissionFilters.permissionFilter(collection, subject), subject);
    }

    /**
     * For a collection whose read access isn't registered in the entities catalog.
     *
     * @param collection       the collection to aggregate
     * @param permissionFilter the documents of the collection the user may read
     * @param subject          the user whose read permissions restrict the related documents. The aggregator throws an
     *                         {@link IllegalArgumentException} for a related collection that isn't registered in the
     *                         entities catalog.
     */
    public MongoAggregator forUser(final String collection,
                                   final PermissionFilter permissionFilter,
                                   final Subject subject) {
        return mongoCollections.aggregator(collection).withPermissions(permissionFilter,
                related -> permissionFilters.permissionFilter(related, subject));
    }
}
