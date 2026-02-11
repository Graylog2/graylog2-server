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
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.utils.CompositeDisplayFormatter;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.security.EntityPermissionsUtils;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.graylog2.shared.security.EntityPermissionsUtils.ID_FIELD;
import static org.graylog2.users.UserImpl.COLLECTION_NAME;
import static org.graylog2.users.UserImpl.LocalAdminUser.LOCAL_ADMIN_ID;
import static org.graylog2.users.UserImpl.USERNAME;

public class MongoEntitySuggestionService implements EntitySuggestionService {

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public MongoEntitySuggestionService(final MongoConnection mongoConnection,
                                        final EntityPermissionsUtils permissionsUtils) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
    }

    private boolean addAdminToSuggestions(final String collection, final String valueColumn, final boolean filterIsEmpty, final String query) {
        return COLLECTION_NAME.equals(collection) && USERNAME.equals(valueColumn) && (filterIsEmpty || LOCAL_ADMIN_ID.contains(query.toLowerCase(Locale.ENGLISH)));
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String targetColumn,
                                            final String valueColumn,
                                            @Nullable final List<String> displayFields,
                                            @Nullable final String displayTemplate,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {
        return suggest(collection, targetColumn, valueColumn, displayFields, displayTemplate, null, query, page, perPage, subject);
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String targetColumn,
                                            final String valueColumn,
                                            @Nullable final List<String> displayFields,
                                            @Nullable final String displayTemplate,
                                            final SearchQueryField.Type identifierType,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collection);
        final boolean filterIsEmpty = Strings.isNullOrEmpty(query);
        final boolean isSpecialCollection = addAdminToSuggestions(collection, valueColumn, filterIsEmpty, query);
        final var isFirstPageAndSpecialCollection = isSpecialCollection && page == 1;
        final var fixNumberOfItemsToReadFromDB = isFirstPageAndSpecialCollection ? 1 : 0;

        // Determine which fields to project
        final Set<String> fieldsToProject = new HashSet<>();
        fieldsToProject.add(targetColumn);

        if (displayFields != null && !displayFields.isEmpty()) {
            fieldsToProject.addAll(displayFields);
        } else {
            fieldsToProject.add(valueColumn);
        }

        final var bsonFilter = !filterIsEmpty
                ? Filters.regex(valueColumn, query, "i")
                : Filters.empty();

        final var resultWithoutPagination = mongoCollection
                .find(bsonFilter)
                .projection(Projections.include(fieldsToProject.toArray(new String[0])))
                .sort(Sorts.ascending(valueColumn));

        final var userCanReadAllEntities = permissionsUtils.hasAllPermission(subject) || permissionsUtils.hasReadPermissionForWholeCollection(subject, collection);
        final var skip = Math.max(0, (page - 1) * perPage - fixNumberOfItemsToReadFromDB);
        final var checkPermission = permissionsUtils.createPermissionCheck(subject, collection);

        final List<EntitySuggestion> suggestions;
        try (final var documents = userCanReadAllEntities
                ? mongoPaginate(resultWithoutPagination, perPage - fixNumberOfItemsToReadFromDB, skip)
                : paginateWithPermissionCheck(resultWithoutPagination, perPage - fixNumberOfItemsToReadFromDB, skip, checkPermission)) {

            final List<EntitySuggestion> staticEntry = isFirstPageAndSpecialCollection ? List.of(new EntitySuggestion(LOCAL_ADMIN_ID, null, "admin")) : List.of();

            final Stream<EntitySuggestion> suggestionsFromDB = documents
                    .map(doc -> {
                        String displayValue;

                        if (displayFields != null && !displayFields.isEmpty()) {
                            displayValue = CompositeDisplayFormatter.format(doc, displayFields, displayTemplate);
                        } else {
                            displayValue = doc.getString(valueColumn);
                        }

                        // Extract targetColumn value as String
                        final String targetValue;
                        final Object rawValue = doc.get(targetColumn);
                        if (rawValue instanceof ObjectId oid) {
                            targetValue = oid.toHexString();
                        } else {
                            targetValue = rawValue != null ? rawValue.toString() : null;
                        }

                        return new EntitySuggestion(
                                doc.getObjectId("_id").toHexString(),
                                targetValue,
                                displayValue
                        );
                    });

            suggestions = Streams.concat(staticEntry.stream(), suggestionsFromDB).toList();
        }

        final long total;
        if (userCanReadAllEntities) {
            total = mongoCollection.countDocuments(bsonFilter);
        } else {
            try (final var stream = MongoUtils.stream(mongoCollection.find(bsonFilter).projection(Projections.include(ID_FIELD)))) {
                total = stream.filter(checkPermission).count();
            }
        }

        return new EntitySuggestionResponse(suggestions,
                PaginatedList.PaginationInfo.create((int) total,
                        suggestions.size(),
                        page,
                        perPage));
    }

    @MustBeClosed
    private Stream<Document> paginateWithPermissionCheck(FindIterable<Document> result, int limit, int skip, Predicate<Document> checkPermission) {
        return MongoUtils.stream(result)
                .filter(checkPermission)
                .limit(limit)
                .skip(skip);
    }

    @MustBeClosed
    private Stream<Document> mongoPaginate(FindIterable<Document> result, int limit, int skip) {
        return MongoUtils.stream(result.limit(limit).skip(skip));
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String targetColumn,
                                            final String valueColumn,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {
        return suggest(collection, targetColumn, valueColumn, null, null, null, query, page, perPage, subject);
    }

}
