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
package org.graylog2.database;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.utils.MongoUtils.insertedId;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;
import static org.graylog2.database.utils.MongoUtils.stream;

/**
 * Tests copied and adjusted from PaginagedDbServiceTest
 */
@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class HelpersAndUtilitiesTest {

    private MongoCollections mongoCollections;
    private MongoCollection<TestDTO> collection;
    private MongoUtils<TestDTO> utils;
    private MongoPaginationHelper<TestDTO> paginationHelper;

    @JsonAutoDetect
    public static class TestDTO {
        @ObjectId
        @Id
        @JsonProperty("id")
        public String id;

        @JsonProperty("title")
        public String title;

        @JsonCreator
        public TestDTO(@JsonProperty("id") @Id String id, @JsonProperty("title") String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("id", id)
                    .add("title", title)
                    .toString();
        }

        public TestDTO(String title) {
            this(null, title);
        }
    }

    private TestDTO newDto(String title) {
        return new TestDTO(title);
    }

    @BeforeEach
    void setUp(MongoDBTestService mongoDBTestService, MongoJackObjectMapperProvider objectMapperProvider) {
        mongoCollections = new MongoCollections(objectMapperProvider, mongoDBTestService.mongoConnection());
        collection = mongoCollections.get("test", TestDTO.class);
        utils = mongoCollections.getUtils(collection);
        paginationHelper = mongoCollections.getPaginationHelper(collection);
    }

    @Test
    public void insertAndGet() {
        final var id = insertedIdAsString(collection.insertOne(newDto("hello")));

        assertThat(id)
                .isInstanceOf(String.class)
                .isNotBlank()
                .matches("^[a-z0-9]{24}$");

        assertThat(utils.getById(id))
                .isPresent()
                .get()
                .extracting("id", "title")
                .containsExactly(id, "hello");
    }

    @Test
    public void delete() {
        final var id = insertedIdAsString(collection.insertOne(newDto("hello")));

        assertThat(utils.deleteById(id)).isTrue();
        assertThat(utils.deleteById(id)).isFalse();

        assertThat(utils.getById(id)).isNotPresent();
    }

    @Test
    public void findPaginatedWithQueryAndSort() {
        collection.insertOne(newDto("hello1"));
        collection.insertOne(newDto("hello2"));
        collection.insertOne(newDto("hello3"));
        collection.insertOne(newDto("hello4"));
        collection.insertOne(newDto("hello5"));

        final PaginatedList<TestDTO> page1 =
                paginationHelper.sort(Sorts.ascending("title")).perPage(2).page(1);

        assertThat(page1.pagination().count()).isEqualTo(2);
        assertThat(page1.pagination().total()).isEqualTo(5);
        assertThat(page1.delegate())
                .extracting("title")
                .containsExactly("hello1", "hello2");

        final PaginatedList<TestDTO> page2 =
                paginationHelper.sort(Sorts.ascending("title")).perPage(2).page(2);

        assertThat(page2.pagination().count()).isEqualTo(2);
        assertThat(page2.pagination().total()).isEqualTo(5);
        assertThat(page2.delegate())
                .extracting("title")
                .containsExactly("hello3", "hello4");

        final PaginatedList<TestDTO> page3 =
                paginationHelper.sort(Sorts.ascending("title")).perPage(2).page(3);

        assertThat(page3.pagination().count()).isEqualTo(1);
        assertThat(page3.pagination().total()).isEqualTo(5);
        assertThat(page3.delegate())
                .extracting("title")
                .containsExactly("hello5");

        final PaginatedList<TestDTO> page1reverse =
                paginationHelper.sort(Sorts.descending("title")).perPage(2).page(1);

        assertThat(page1reverse.pagination().count()).isEqualTo(2);
        assertThat(page1reverse.pagination().total()).isEqualTo(5);
        assertThat(page1reverse.delegate())
                .extracting("title")
                .containsExactly("hello5", "hello4");
    }

    @Test
    public void findPaginatedWithQueryFilterAndSort() {
        collection.insertOne(newDto("hello1"));
        collection.insertOne(newDto("hello2"));
        collection.insertOne(newDto("hello3"));
        collection.insertOne(newDto("hello4"));
        collection.insertOne(newDto("hello5"));
        collection.insertOne(newDto("hello6"));
        collection.insertOne(newDto("hello7"));

        final Predicate<TestDTO> filter = view -> view.title.matches("hello[23456]");

        final PaginatedList<TestDTO> page1 = paginationHelper.sort("title", "asc").perPage(2)
                .postProcessedPage(1, filter);

        assertThat(page1.pagination().count()).isEqualTo(2);
        assertThat(page1.pagination().total()).isEqualTo(5);
        assertThat(page1.delegate())
                .extracting("title")
                .containsExactly("hello2", "hello3");

        final PaginatedList<TestDTO> page2 = paginationHelper.sort("title", "asc").perPage(2)
                .postProcessedPage(2, filter);

        assertThat(page2.pagination().count()).isEqualTo(2);
        assertThat(page2.pagination().total()).isEqualTo(5);
        assertThat(page2.delegate())
                .extracting("title")
                .containsExactly("hello4", "hello5");

        final PaginatedList<TestDTO> page3 = paginationHelper.sort("title", "asc").perPage(2)
                .postProcessedPage(3, filter);
        assertThat(page3.pagination().count()).isEqualTo(1);
        assertThat(page3.pagination().total()).isEqualTo(5);
        assertThat(page3.delegate())
                .extracting("title")
                .containsExactly("hello6");

        final PaginatedList<TestDTO> page4 = paginationHelper.sort("title", "asc").perPage(4)
                .postProcessedPage(2, filter);

        assertThat(page4.pagination().count()).isEqualTo(1);
        assertThat(page4.pagination().total()).isEqualTo(5);
        assertThat(page4.delegate())
                .extracting("title")
                .containsExactly("hello6");

        final PaginatedList<TestDTO> page1reverse = paginationHelper.sort("title", "desc")
                .perPage(2).postProcessedPage(1, filter);

        assertThat(page1reverse.pagination().count()).isEqualTo(2);
        assertThat(page1reverse.pagination().total()).isEqualTo(5);
        assertThat(page1reverse.delegate())
                .extracting("title")
                .containsExactly("hello6", "hello5");
    }

    @Test
    public void streamAll() {
        collection.insertOne(newDto("hello1"));
        collection.insertOne(newDto("hello2"));
        collection.insertOne(newDto("hello3"));
        collection.insertOne(newDto("hello4"));

        assertThat(stream(collection.find()).toList())
                .hasSize(4)
                .extracting("title")
                .containsExactly("hello1", "hello2", "hello3", "hello4");
    }


    @Test
    public void streamByIds() {
        final var hello1Id = insertedId(collection.insertOne(newDto("hello1")));
        final var hello2Id = insertedId(collection.insertOne(newDto("hello2")));
        final var hello3Id = insertedId(collection.insertOne(newDto("hello3")));
        collection.insertOne(newDto("hello5"));
        collection.insertOne(newDto("hello5"));

        try (final var stream = stream(collection.find(Filters.in("_id", hello1Id, hello2Id, hello3Id)))) {
            assertThat(stream.toList())
                    .hasSize(3)
                    .extracting("title")
                    .containsExactly("hello1", "hello2", "hello3");
        }
    }

    @Test
    public void streamByQuery() {
        collection.insertOne(newDto("hello1"));
        collection.insertOne(newDto("hello2"));
        collection.insertOne(newDto("hello3"));
        collection.insertOne(newDto("hello4"));
        collection.insertOne(newDto("hello5"));

        try (final var stream = stream(collection.find(Filters.in("title", "hello1", "hello3", "hello4")))) {
            assertThat(stream.toList())
                    .hasSize(3)
                    .extracting("title")
                    .containsExactly("hello1", "hello3", "hello4");
        }
    }

    @Test
    public void streamQueryWithSort() {
        collection.insertOne(newDto("hello1"));
        collection.insertOne(newDto("hello2"));
        collection.insertOne(newDto("hello3"));
        collection.insertOne(newDto("hello4"));
        collection.insertOne(newDto("hello5"));

        try (final var stream = stream(collection
                .find(Filters.in("title", "hello5", "hello3", "hello1"))
                .sort(Sorts.descending("title")))) {
            assertThat(stream.toList())
                    .hasSize(3)
                    .extracting("title")
                    .containsExactly("hello5", "hello3", "hello1");
        }
    }
}
