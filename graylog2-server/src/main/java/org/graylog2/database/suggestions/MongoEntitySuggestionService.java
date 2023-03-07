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

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.Subject;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Suggestions won't be provided to users that have permissions to just some of the entities in the collection
public class MongoEntitySuggestionService implements EntitySuggestionService {

    private final MongoConnection mongoConnection;
    //TODO: It would be great to have a mechanism to automatically map collection to proper permission. For now this mapping will be hardcoded for supported collections.
    private final Map<String, String> mongoCollectionToPermission = new HashMap<>();

    @Inject
    public MongoEntitySuggestionService(final MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
        this.mongoCollectionToPermission.put(MongoIndexSetService.COLLECTION_NAME, RestPermissions.INDEXSETS_READ);
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String valueColumn,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {

        if (!hasAllPermission(subject) && !hasReadPermissionForWholeCollection(subject, collection)) {
            return new EntitySuggestionResponse(List.of(),
                    PaginatedList.PaginationInfo.create(0, 0, page, perPage));
        }

        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collection);

        Bson bsonFilter = (query != null && !query.isEmpty()) ?
                Filters.regex(valueColumn, query, "i") :
                new BsonDocument();

        final FindIterable<Document> documents = mongoCollection
                .find(bsonFilter)
                .projection(Projections.include(valueColumn))
                .sort(Sorts.ascending(valueColumn))
                .limit(perPage)
                .skip((page - 1) * perPage);

        final List<EntitySuggestion> suggestions = documents
                .map(doc ->
                        new EntitySuggestion(
                                doc.getObjectId("_id").toString(),
                                doc.getString(valueColumn)
                        )
                )
                .into(new ArrayList<>());

        final long total = mongoCollection.countDocuments(bsonFilter);

        return new EntitySuggestionResponse(suggestions,
                PaginatedList.PaginationInfo.create((int) total,
                        suggestions.size(),
                        page,
                        perPage));

    }

    public boolean hasAllPermission(final Subject subject) {
        return subject.isPermitted(new AllPermission());
    }

    public boolean hasReadPermissionForWholeCollection(final Subject subject,
                                                       final String collection) {
        final String permission = mongoCollectionToPermission.get(collection);
        if (permission == null) {
            return false;
        }
        return subject.isPermitted(permission + ":*");
    }


}
