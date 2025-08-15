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
package org.graylog2.database.utils;

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UnwindOptions;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.entities.EntitySource;
import org.graylog2.database.entities.SourcedMongoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.include;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.graylog2.database.utils.MongoUtils.stream;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@MongoDBFixtures("entity-source-testing.json")
class EntitySourceAggregationTest {
    private static final List<Bson> LOOKUP_PIPELINE = List.of(
            match(Filters.expr(new Document("$eq", List.of("$entity_id", "$$vid")))),
            project(Projections.fields(excludeId(), include(EntitySource.FIELD_SOURCE, EntitySource.FIELD_PARENT_ID))),
            limit(1)
    );

    private static final Bson LOOKUP = Aggregates.lookup(
            "entity_source",
            List.of(new Variable<>("vid", "$_id")),
            LOOKUP_PIPELINE,
            SourcedMongoEntity.FIELD_ENTITY_SOURCE
    );

    private static final Bson UNWIND = Aggregates.unwind("$entity_source",
            new UnwindOptions().preserveNullAndEmptyArrays(true));

    private MongoCollection<ViewDTO> collection;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        final MongoCollections mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        collection = mongoCollections.collection("views", ViewDTO.class);
    }

    @Test
    void testLookupWithSource() {
        try (var stream = stream(collection.aggregate(List.of(LOOKUP, UNWIND)))) {
            final Map<String, ViewDTO> viewMap = stream.collect(Collectors.toMap(ViewDTO::id, Function.identity()));

            final ViewDTO illuminateDashboard = viewMap.get("6890b706dc8217538b763006");
            assertThat(illuminateDashboard)
                    .isNotNull()
                    .satisfies(view -> {
                        assertThat(view.entitySource()).isPresent();
                        assertThat(view.entitySource().get().source()).isEqualTo("ILLUMINATE");
                        assertThat(view.entitySource().get().parentId()).isEmpty();
                    });

            final ViewDTO clonedDashboard = viewMap.get("6824a95255eb3731adc1e552");
            assertThat(clonedDashboard)
                    .isNotNull()
                    .satisfies(view -> {
                        assertThat(view.entitySource()).isPresent();
                        assertThat(view.entitySource().get().source()).isEqualTo(EntitySource.USER_DEFINED);
                        assertThat(view.entitySource().get().parentId()).isEqualTo(Optional.of("0a05ab4d-c0d0-4a4f-aab3-e0c300991c10"));
                    });

            final ViewDTO otherDashboard1 = viewMap.get("684082a26487e66561c02ac5");
            final ViewDTO otherDashboard2 = viewMap.get("6822125135a24c5405fdeabc");
            assertThat(otherDashboard1)
                    .isNotNull()
                    .satisfies(view -> assertThat(view.entitySource()).isEmpty());
            assertThat(otherDashboard2)
                    .isNotNull()
                    .satisfies(view -> assertThat(view.entitySource()).isEmpty());
        }
    }
}
