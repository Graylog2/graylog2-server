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
package org.graylog2.streams;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.mongojack.DBQuery;

import jakarta.inject.Inject;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

public class PaginatedStreamService extends PaginatedDbService<StreamDTO> {
    private static final String COLLECTION_NAME = "streams";
    private static final List<String> STRING_FIELDS = List.of("title", "description", "index_set_title");
    private final MongoCollection<Document> collection;

    @Inject
    public PaginatedStreamService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, StreamDTO.class, COLLECTION_NAME);
        this.collection = mongoConnection.getMongoDatabase().getCollection(COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    public PaginatedList<StreamDTO> findPaginated(Bson dbQuery, //query executed on DB level
                                                  Predicate<StreamDTO> predicate, //predicate executed on code level, AFTER data is fetched
                                                  int page,
                                                  int perPage,
                                                  String sortField,
                                                  String order) {


        var pipelineBuilder = ImmutableList.<Bson>builder()
                .add(Aggregates.match(dbQuery));

        if (sortField.equals("index_set_title")) {
            pipelineBuilder.add(Aggregates.lookup(
                            MongoIndexSetService.COLLECTION_NAME,
                            List.of(new Variable<>("index_set_id", doc("$toObjectId", "$index_set_id"))),
                            List.of(Aggregates.match(doc("$expr", doc("$eq", List.of("$_id", "$$index_set_id"))))),
                            "index_set"
                    ))
                    .add(Aggregates.set(new Field<>("index_set_title", doc("$first", "$index_set.title"))))
                    .add(Aggregates.unset("index_set"));
        }

        if (isStringField(sortField)) {
            pipelineBuilder.add(Aggregates.set(new Field<>("lower" + sortField, doc("$toLower", "$" + sortField))))
                    .add(Aggregates.sort(getSortBuilder(order, "lower" + sortField)))
                    .add(Aggregates.unset("lower" + sortField));
        } else {
            pipelineBuilder.add(Aggregates.sort(getSortBuilder(order, sortField)));
        }

        final AggregateIterable<Document> result = collection.aggregate(pipelineBuilder.build());

        final List<StreamDTO> streamsList = StreamSupport.stream(result.spliterator(), false)
                .map(StreamDTO::fromDocument)
                .filter(predicate)
                .toList();

        final long grandTotal = db.find(DBQuery.empty()).toArray()
                .stream()
                .filter(predicate)
                .count();

        final List<StreamDTO> paginatedStreams = perPage > 0
                ? streamsList.stream()
                .skip((long) perPage * Math.max(0, page - 1))
                .limit(perPage)
                .toList()
                : streamsList;

        return new PaginatedList<>(paginatedStreams, streamsList.size(), page, perPage, grandTotal);

    }

    private boolean isStringField(String sortField) {
        return STRING_FIELDS.contains(sortField);
    }

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }
}
