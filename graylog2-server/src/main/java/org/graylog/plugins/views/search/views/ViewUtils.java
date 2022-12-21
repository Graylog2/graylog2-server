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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.plugins.views.favorites.FavoritesService;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.mongojack.DBSort;

import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface ViewUtils<T> {
    ObjectMapper mapper();
    MongoCollection<Document> collection();
    Class<T> type();

    default T deserialize(final Document document) {
        try {
            // replace "_id" with "id", because the ViewDTO depends on it
            if(document.containsKey("_id")) {
                final var id = document.get("_id");
                document.remove("_id");
                document.put("id", id);
            }
            var json = mapper().writeValueAsString(document);
            return mapper().readValue(json, type());
        } catch (JsonProcessingException jpe) {
            throw new RuntimeException("Could not deserialize view: " + jpe.getMessage(), jpe);
        }
    }

    default Stream<Document> findViews(SearchUser searchUser,
                                         Bson query,
                                         DBSort.SortBuilder sort) {
        var user = searchUser.getUser().getId();
        final AggregateIterable<Document> result = collection().aggregate(List.of(
                        Aggregates.match(query),
                        Aggregates.lookup(
                                FavoritesService.COLLECTION_NAME,
                                List.of(
                                        new Variable<>("searchId", doc("$toString", "$_id")),
                                        new Variable<>("userId", user)
                                ),
                                List.of(Aggregates.unwind("$items"),
                                        Aggregates.match(
                                                doc("$expr", doc("$and", List.of(
                                                                doc("$eq", List.of("$items.id", "$$searchId")),
                                                                doc("$eq", List.of("$user_id", "$$userId"))
                                                        )
                                                ))),
                                        Aggregates.project(doc("_id", 1))
                                ),
                                "favorites"
                        ),
                        Aggregates.set(new Field<>("favorite", doc("$gt", List.of(doc("$size", "$favorites"), 0)))),
                        Aggregates.unset("favorites"),
                        Aggregates.sort(sort)
                )
        );

        return StreamSupport.stream(result.spliterator(), false);
    }

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }
}
