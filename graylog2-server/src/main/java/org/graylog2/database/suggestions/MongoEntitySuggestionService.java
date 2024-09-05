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
import com.google.common.collect.Streams;
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

    private boolean addAdminToSuggestions(final String collection, final String valueColumn) {
        return "users".equals(collection) && "username".equals(valueColumn);
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String valueColumn,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collection);
        final boolean addAdminToSuggestions = addAdminToSuggestions(collection, valueColumn);

        final var bsonFilter = !Strings.isNullOrEmpty(query)
                ? Filters.regex(valueColumn, query, "i")
                : Filters.empty();

        final var resultWithoutPagination = mongoCollection
                .find(bsonFilter)
                .projection(Projections.include(valueColumn))
                .sort(Sorts.ascending(valueColumn));

        final var addAddminCheck = addAdminToSuggestions && page == 1;

        final var userCanReadAllEntities = permissionsUtils.hasAllPermission(subject) || permissionsUtils.hasReadPermissionForWholeCollection(subject, collection);
        final var skip = Math.max(0, (page - 1) * perPage - (addAdminToSuggestions ? 1 : 0));
        final var checkPermission = permissionsUtils.createPermissionCheck(subject, collection);
        final var documents = userCanReadAllEntities
                ? mongoPaginate(resultWithoutPagination, perPage - (addAddminCheck ? 1 : 0), skip)
                : paginateWithPermissionCheck(resultWithoutPagination, perPage - (addAddminCheck ? 1 : 0), skip, checkPermission);

        final List<EntitySuggestion> staticEntry = addAddminCheck ? List.of(new EntitySuggestion("admin", "admin")) : List.of();

        final Stream<EntitySuggestion> suggestionsFromDB = documents
                .map(doc ->
                        new EntitySuggestion(
                                doc.getObjectId(ID_FIELD).toString(),
                                doc.getString(valueColumn)
                        )
                );

        final List<EntitySuggestion> suggestions = Streams.concat(staticEntry.stream(), suggestionsFromDB).toList();

        final long total = userCanReadAllEntities
                ? mongoCollection.countDocuments(bsonFilter)
                : MongoUtils.stream(mongoCollection.find(bsonFilter).projection(Projections.include(ID_FIELD))).filter(checkPermission).count();

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
