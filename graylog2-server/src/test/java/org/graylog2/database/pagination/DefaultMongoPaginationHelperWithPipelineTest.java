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
package org.graylog2.database.pagination;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Variable;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@MongoDBFixtures("joined-collections.json")
class DefaultMongoPaginationHelperWithPipelineTest {
    private record DTO(String id, String name, DateTime lastRun) implements MongoEntity {}

    private static final String LAST_RUN = "last_run";

    private static final DTO bar = new DTO("67fcc1dfd67032f27e0e9d71", "Bar", new DateTime("2025-04-12T04:36:54.352Z", DateTimeZone.UTC));
    private static final DTO foo = new DTO("67fcc1c4d67032f27e0e9d70", "Foo", new DateTime("2025-04-14T04:36:54.352Z", DateTimeZone.UTC));

    private MongoPaginationHelper<DTO> paginationHelper;

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        final MongoCollections mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        final MongoCollection<DTO> collection = mongoCollections.collection("test", DTO.class);
        paginationHelper = new DefaultMongoPaginationHelper<>(collection);
    }

    @Test
    void sortsByLastDate() {
        final var pipeline = paginationHelper.pipeline(List.of(
                Aggregates.lookup(
                        "test_runs",
                        List.of(new Variable<>("testId", new Document("$toString", "$_id"))),
                        List.<Bson>of(new Document("$match", new Document("$expr", new Document("$eq", List.of("$test_id", "$$testId"))))),
                        "runs"
                ),
                Aggregates.addFields(new Field<>(LAST_RUN, new Document("$max", "$runs.created_at"))),
                Aggregates.project(Projections.exclude("runs"))
        ));

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(10)
                .page(1)
                .delegate())
                .containsExactly(bar, foo);

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(10)
                .page(1)
                .delegate())
                .containsExactly(foo, bar);

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(1)
                .page(1)
                .delegate())
                .containsExactly(bar);

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(1)
                .page(2)
                .delegate())
                .containsExactly(foo);

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(1)
                .page(3)
                .delegate())
                .isEmpty();

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(1)
                .page(1)
                .delegate())
                .containsExactly(foo);

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(1)
                .page(2)
                .delegate())
                .containsExactly(bar);

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(1)
                .page(3)
                .delegate())
                .isEmpty();
    }

    @Test
    void sortsByLastDateWithFilter() {
        final var pipeline = paginationHelper.pipeline(List.of(
                Aggregates.lookup(
                        "test_runs",
                        List.of(new Variable<>("testId", new Document("$toString", "$_id"))),
                        List.of(new Document("$match", new Document("$expr", new Document("$eq", List.of("$test_id", "$$testId"))))),
                        "runs"
                ),
                Aggregates.addFields(new Field<>(LAST_RUN, new Document("$max", "$runs.created_at"))),
                Aggregates.project(Projections.exclude("runs"))
        )).filter(Filters.eq("_id", new ObjectId("67fcc1dfd67032f27e0e9d71")));

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(10)
                .page(1)
                .delegate())
                .containsExactly(bar);

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(10)
                .page(1)
                .delegate())
                .containsExactly(bar);

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(1)
                .page(1)
                .delegate())
                .containsExactly(bar);

        assertThat(pipeline
                .sort(Sorts.ascending(LAST_RUN))
                .perPage(1)
                .page(2)
                .delegate())
                .isEmpty();

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(1)
                .page(1)
                .delegate())
                .containsExactly(bar);

        assertThat(pipeline
                .sort(Sorts.descending(LAST_RUN))
                .perPage(1)
                .page(2)
                .delegate())
                .isEmpty();
    }
}
