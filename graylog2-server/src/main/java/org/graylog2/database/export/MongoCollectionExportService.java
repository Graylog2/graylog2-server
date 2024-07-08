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
package org.graylog2.database.export;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.plugins.views.search.searchtypes.Sort;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A general service for exporting limited list of Documents (with a selection of fields) from a collection in MongoDB.
 * Meant to be used when users have permissions to read the whole entity collection.
 * It will work with selective permissions as well (users having permissions to view some entities in the collection),
 * but performance may be low in that particular case.
 */
public class MongoCollectionExportService {

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public MongoCollectionExportService(final MongoConnection mongoConnection,
                                        final EntityPermissionsUtils permissionsUtils) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
    }

    public List<Document> export(final String collectionName,
                                 final List<String> exportedFieldNames,
                                 final int limit,
                                 final Bson dbFilter,
                                 final List<Sort> sorts,
                                 final Subject subject) {
        final MongoCollection<Document> collection = mongoConnection.getMongoDatabase().getCollection(collectionName);
        final FindIterable<Document> resultsWithoutLimit = collection.find(Objects.requireNonNullElse(dbFilter, Filters.empty()))
                .projection(Projections.fields(Projections.include(exportedFieldNames)))
                .sort(toMongoDbSort(sorts));

        final var userCanReadAllEntities = permissionsUtils.hasAllPermission(subject) || permissionsUtils.hasReadPermissionForWholeCollection(subject, collectionName);
        final var checkPermission = permissionsUtils.createPermissionCheck(subject, collectionName);
        final var documents = userCanReadAllEntities
                ? getFromMongo(resultsWithoutLimit, limit)
                : getWithInMemoryPermissionCheck(resultsWithoutLimit, limit, checkPermission);

        return documents.collect(Collectors.toList());

    }

    private Bson toMongoDbSort(final List<Sort> sorts) {
        return Sorts.orderBy(sorts.stream()
                .map(srt -> srt.order() == Sort.Order.DESC ?
                        Sorts.descending(srt.field()) : Sorts.ascending(srt.field()))
                .toList());
    }

    private Stream<Document> getWithInMemoryPermissionCheck(FindIterable<Document> result, int limit, Predicate<Document> checkPermission) {
        return MongoUtils.stream(result)
                .filter(checkPermission)
                .limit(limit);
    }

    private Stream<Document> getFromMongo(FindIterable<Document> result, int limit) {
        return MongoUtils.stream(result.limit(limit));
    }
}
