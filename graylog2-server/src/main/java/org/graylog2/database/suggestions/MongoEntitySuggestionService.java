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
import org.apache.shiro.subject.Subject;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedList;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

//TODO: permissions
//TODO: collection name -> permission name mapping
public class MongoEntitySuggestionService implements EntitySuggestionService {

    private final MongoConnection mongoConnection;

    @Inject
    public MongoEntitySuggestionService(final MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    @Override
    public EntitySuggestionResponse suggest(final String collection,
                                            final String valueColumn,
                                            final String query,
                                            final int page,
                                            final int perPage,
                                            final Subject subject) {
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


}
