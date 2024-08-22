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
package org.graylog2.database.suggestions;

import com.google.common.base.Strings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.graylog2.shared.security.EntityPermissionsUtils.ID_FIELD;

public class MongoEntitySuggestionService implements EntitySuggestionService {

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public MongoEntitySuggestionService(final MongoConnection mongoConnection,
                                        final EntityPermissionsUtils permissionsUtils) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String valueColumn,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject,
                                            final List<String> staticEntries) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collection);

        final var bsonFilter = !Strings.isNullOrEmpty(query)
                ? Filters.regex(valueColumn, query, "i")
                : Filters.empty();

        final var resultWithoutPagination = mongoCollection
                .find(bsonFilter)
                .projection(Projections.include(valueColumn))
                .sort(Sorts.ascending(valueColumn));

        final var userCanReadAllEntities = permissionsUtils.hasAllPermission(subject) || permissionsUtils.hasReadPermissionForWholeCollection(subject, collection);

        final var lengthStaticEntries = staticEntries.size();
        final var start = (page - 1) * perPage;

        final var skip = start < lengthStaticEntries ? 0 : start - lengthStaticEntries;
        final var limit = perPage - (skip % perPage);

        final var checkPermission = permissionsUtils.createPermissionCheck(subject, collection);
        final var documents = userCanReadAllEntities
                ? mongoPaginate(resultWithoutPagination, limit, skip)
                : paginateWithPermissionCheck(resultWithoutPagination, limit, skip, checkPermission);

        final var from = start > lengthStaticEntries ? 0 : start;
        final var to = start > lengthStaticEntries ? 0 : start + perPage > lengthStaticEntries ? lengthStaticEntries : start + page;

        final List<EntitySuggestion> staticEntriesList = staticEntries.subList(from, to).stream().map(e -> new EntitySuggestion(e, e)).toList();

        final List<EntitySuggestion> suggestionsFromMongo = documents
                .map(doc ->
                        new EntitySuggestion(
                                doc.getObjectId(ID_FIELD).toString(),
                                doc.getString(valueColumn)
                        )
                )
                .toList();

        final List<EntitySuggestion> suggestions = Stream.of(staticEntriesList, suggestionsFromMongo).flatMap(java.util.Collection::stream).toList();

        final long total = (userCanReadAllEntities
                ? mongoCollection.countDocuments(bsonFilter)
                : MongoUtils.stream(mongoCollection.find(bsonFilter).projection(Projections.include(ID_FIELD))).filter(checkPermission).count()) + lengthStaticEntries;

        return new EntitySuggestionResponse(suggestions,
                PaginatedList.PaginationInfo.create((int) total,
                        suggestions.size(),
                        page,
                        perPage));
    }

    private Stream<Document> paginateWithPermissionCheck(FindIterable<Document> result, int limit, int skip, Predicate<Document> checkPermission) {
        return MongoUtils.stream(result)
                .filter(checkPermission)
                .limit(limit)
                .skip(skip);
    }

    private Stream<Document> mongoPaginate(FindIterable<Document> result, int limit, int skip) {
        return MongoUtils.stream(result.limit(limit).skip(skip));
    }

}
