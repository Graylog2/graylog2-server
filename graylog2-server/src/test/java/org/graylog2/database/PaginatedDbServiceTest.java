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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class PaginatedDbServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @JsonAutoDetect
    public static class TestDTO {
        @ObjectId
        @Id
        @JsonProperty("id")
        public String id;

        @JsonProperty("title")
        public String title;

        @JsonCreator
        public TestDTO(@JsonProperty("id") String id, @JsonProperty("title") String title) {
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

    public static class TestDbService extends PaginatedDbService<TestDTO> {
        protected TestDbService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
            super(mongoConnection, mapper, TestDTO.class, "db_service_test");
        }
    }

    private TestDbService dbService;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        this.dbService = new TestDbService(mongodb.mongoConnection(), objectMapperProvider);
    }

    @After
    public void tearDown() {
        mongodb.mongoConnection().getMongoDatabase().drop();
    }

    private TestDTO newDto(String title) {
        return new TestDTO(title);
    }

    @Test
    public void saveAndGet() {
        final TestDTO savedDto = dbService.save(newDto("hello"));

        assertThat(savedDto.title).isEqualTo("hello");
        assertThat(savedDto.id)
                .isInstanceOf(String.class)
                .isNotBlank()
                .matches("^[a-z0-9]{24}$");

        assertThat(dbService.get(savedDto.id))
                .isPresent()
                .get()
                .extracting("id", "title")
                .containsExactly(savedDto.id, "hello");
    }

    @Test
    public void delete() {
        final TestDTO savedDto = dbService.save(newDto("hello"));

        assertThat(dbService.delete(savedDto.id)).isEqualTo(1);
        assertThat(dbService.delete(savedDto.id)).isEqualTo(0);

        assertThat(dbService.get(savedDto.id)).isNotPresent();
    }

    @Test
    public void findPaginatedWithQueryAndSort() {
        dbService.save(newDto("hello1"));
        dbService.save(newDto("hello2"));
        dbService.save(newDto("hello3"));
        dbService.save(newDto("hello4"));
        dbService.save(newDto("hello5"));

        final PaginatedList<TestDTO> page1 = dbService.findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.asc("title"), 1, 2);

        assertThat(page1.pagination().count()).isEqualTo(2);
        assertThat(page1.pagination().total()).isEqualTo(5);
        assertThat(page1.delegate())
                .extracting("title")
                .containsExactly("hello1", "hello2");

        final PaginatedList<TestDTO> page2 = dbService.findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.asc("title"), 2, 2);

        assertThat(page2.pagination().count()).isEqualTo(2);
        assertThat(page2.pagination().total()).isEqualTo(5);
        assertThat(page2.delegate())
                .extracting("title")
                .containsExactly("hello3", "hello4");

        final PaginatedList<TestDTO> page3 = dbService.findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.asc("title"), 3, 2);

        assertThat(page3.pagination().count()).isEqualTo(1);
        assertThat(page3.pagination().total()).isEqualTo(5);
        assertThat(page3.delegate())
                .extracting("title")
                .containsExactly("hello5");

        final PaginatedList<TestDTO> page1reverse = dbService.findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.desc("title"), 1, 2);

        assertThat(page1reverse.pagination().count()).isEqualTo(2);
        assertThat(page1reverse.pagination().total()).isEqualTo(5);
        assertThat(page1reverse.delegate())
                .extracting("title")
                .containsExactly("hello5", "hello4");
    }

    @Test
    public void findPaginatedWithQueryFilterAndSort() {
        dbService.save(newDto("hello1"));
        dbService.save(newDto("hello2"));
        dbService.save(newDto("hello3"));
        dbService.save(newDto("hello4"));
        dbService.save(newDto("hello5"));
        dbService.save(newDto("hello6"));
        dbService.save(newDto("hello7"));

        final Predicate<TestDTO> filter = view -> view.title.matches("hello[23456]");

        final PaginatedList<TestDTO> page1 = dbService.findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, DBSort.asc("title"), 1, 2);

        assertThat(page1.pagination().count()).isEqualTo(2);
        assertThat(page1.pagination().total()).isEqualTo(5);
        assertThat(page1.delegate())
                .extracting("title")
                .containsExactly("hello2", "hello3");

        final PaginatedList<TestDTO> page2 = dbService.findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, DBSort.asc("title"), 2, 2);

        assertThat(page2.pagination().count()).isEqualTo(2);
        assertThat(page2.pagination().total()).isEqualTo(5);
        assertThat(page2.delegate())
                .extracting("title")
                .containsExactly("hello4", "hello5");

        final PaginatedList<TestDTO> page3 = dbService.findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, DBSort.asc("title"), 3, 2);

        assertThat(page3.pagination().count()).isEqualTo(1);
        assertThat(page3.pagination().total()).isEqualTo(5);
        assertThat(page3.delegate())
                .extracting("title")
                .containsExactly("hello6");

        final PaginatedList<TestDTO> page4 = dbService.findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, DBSort.asc("title"), 2, 4);

        assertThat(page4.pagination().count()).isEqualTo(1);
        assertThat(page4.pagination().total()).isEqualTo(5);
        assertThat(page4.delegate())
                .extracting("title")
                .containsExactly("hello6");

        final PaginatedList<TestDTO> page1reverse = dbService.findPaginatedWithQueryFilterAndSort(DBQuery.empty(), filter, DBSort.desc("title"), 1, 2);

        assertThat(page1reverse.pagination().count()).isEqualTo(2);
        assertThat(page1reverse.pagination().total()).isEqualTo(5);
        assertThat(page1reverse.delegate())
                .extracting("title")
                .containsExactly("hello6", "hello5");
    }

    @Test
    public void streamAll() {
        dbService.save(newDto("hello1"));
        dbService.save(newDto("hello2"));
        dbService.save(newDto("hello3"));
        dbService.save(newDto("hello4"));

        try (final Stream<TestDTO> cursor = dbService.streamAll()) {
            assertThat(cursor.collect(Collectors.toList()))
                    .hasSize(4)
                    .extracting("title")
                    .containsExactly("hello1", "hello2", "hello3", "hello4");
        }
    }

    @Test
    public void streamByIds() {
        final TestDTO hello1 = dbService.save(newDto("hello1"));
        final TestDTO hello2 = dbService.save(newDto("hello2"));
        final TestDTO hello3 = dbService.save(newDto("hello3"));
        dbService.save(newDto("hello5"));
        dbService.save(newDto("hello5"));

        try (final Stream<TestDTO> cursor = dbService.streamByIds(ImmutableSet.of(hello1.id, hello2.id, hello3.id))) {
            final List<TestDTO> list = cursor.collect(Collectors.toList());

            assertThat(list)
                    .hasSize(3)
                    .extracting("title")
                    .containsExactly("hello1", "hello2", "hello3");
        }
    }

    @Test
    public void streamQuery() {
        dbService.save(newDto("hello1"));
        dbService.save(newDto("hello2"));
        dbService.save(newDto("hello3"));
        dbService.save(newDto("hello4"));
        dbService.save(newDto("hello5"));

        final DBQuery.Query query = DBQuery.in("title", "hello1", "hello3", "hello4");

        try (final Stream<TestDTO> cursor = dbService.streamQuery(query)) {
            final List<TestDTO> list = cursor.collect(Collectors.toList());

            assertThat(list)
                    .hasSize(3)
                    .extracting("title")
                    .containsExactly("hello1", "hello3", "hello4");
        }
    }

    @Test
    public void streamQueryWithSort() {
        dbService.save(newDto("hello1"));
        dbService.save(newDto("hello2"));
        dbService.save(newDto("hello3"));
        dbService.save(newDto("hello4"));
        dbService.save(newDto("hello5"));

        final DBQuery.Query query = DBQuery.in("title", "hello5", "hello3", "hello1");
        final DBSort.SortBuilder sort = DBSort.desc("title");

        try (final Stream<TestDTO> cursor = dbService.streamQueryWithSort(query, sort)) {
            final List<TestDTO> list = cursor.collect(Collectors.toList());

            assertThat(list)
                    .hasSize(3)
                    .extracting("title")
                    .containsExactly("hello5", "hello3", "hello1");
        }
    }

    @Test
    public void sortBuilder() {
        assertThat(dbService.getSortBuilder("asc", "f")).isEqualTo(DBSort.asc("f"));
        assertThat(dbService.getSortBuilder("aSc", "f")).isEqualTo(DBSort.asc("f"));
        assertThat(dbService.getSortBuilder("desc", "f")).isEqualTo(DBSort.desc("f"));
        assertThat(dbService.getSortBuilder("dEsC", "f")).isEqualTo(DBSort.desc("f"));
    }
}
