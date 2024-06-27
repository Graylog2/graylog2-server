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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.database.utils.MongoUtils;

import java.util.List;
import java.util.Optional;

//TODO: Suggestions won't be provided to users that have permissions to just some of the entities in the collection
public class MongoEntitySuggestionService implements EntitySuggestionService {
    private static final String ID_FIELD = "_id";

    private final MongoConnection mongoConnection;
    private final DbEntitiesCatalog catalog;

    @Inject
    public MongoEntitySuggestionService(final MongoConnection mongoConnection, final DbEntitiesCatalog catalog) {
        this.mongoConnection = mongoConnection;
        this.catalog = catalog;
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String valueColumn,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collection);

        final var bsonFilter = (query != null && !query.isEmpty())
                ? Filters.regex(valueColumn, query, "i")
                : Filters.empty();

        final var resultWithoutPagination = mongoCollection
                .find(bsonFilter)
                .projection(Projections.include(valueColumn))
                .sort(Sorts.ascending(valueColumn));

        final var readPermission = readPermissionForCollection(collection);
        final var userCanReadAllEntities = hasAllPermission(subject) || hasReadPermissionForWholeCollection(subject, collection);
        final var documents = userCanReadAllEntities
                ? MongoUtils.stream(resultWithoutPagination
                .limit(perPage)
                .skip((page - 1) * perPage))
                : MongoUtils.stream(resultWithoutPagination)
                .filter(doc -> readPermission.map(permission -> subject.isPermitted(permission + ":" + doc.getObjectId(ID_FIELD).toString())).orElse(false))
                .limit(perPage)
                .skip((long) (page - 1) * perPage);

        final List<EntitySuggestion> suggestions = documents
                .map(doc ->
                        new EntitySuggestion(
                                doc.getObjectId(ID_FIELD).toString(),
                                doc.getString(valueColumn)
                        )
                )
                .toList();

        final long total = userCanReadAllEntities
                ? mongoCollection.countDocuments(bsonFilter)
                : MongoUtils.stream(mongoCollection.find(bsonFilter))
                .filter(doc -> readPermission.map(permission -> subject.isPermitted(permission + ":" + doc.getObjectId(ID_FIELD).toString())).orElse(false))
                .count();

        return new EntitySuggestionResponse(suggestions,
                PaginatedList.PaginationInfo.create((int) total,
                        suggestions.size(),
                        page,
                        perPage));
    }

    boolean hasAllPermission(final Subject subject) {
        return subject.isPermitted(new AllPermission());
    }

    boolean hasReadPermissionForWholeCollection(final Subject subject,
                                                final String collection) {
        return readPermissionForCollection(collection)
                .map(rp -> rp.equals(DbEntity.ALL_ALLOWED) || subject.isPermitted(rp + ":*"))
                .orElse(false);
    }

    private Optional<String> readPermissionForCollection(String collection) {
        return catalog.getByCollectionName(collection)
                .map(DbEntityCatalogEntry::readPermission);
    }
}
