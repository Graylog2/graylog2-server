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
package org.graylog2.indexer.fieldtypes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class IndexFieldTypesServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private IndexFieldTypesService dbService;

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        final StreamService streamService = mock(StreamService.class);
        this.dbService = new IndexFieldTypesService(mongodb.mongoConnection(), streamService, objectMapperProvider);
    }

    @After
    public void tearDown() {
        mongodb.mongoConnection().getMongoDatabase().drop();
    }

    private IndexFieldTypesDTO createDto(String indexName, String indexSetId, Set<FieldTypeDTO> fields) {
        return IndexFieldTypesDTO.builder()
                .indexName(indexName)
                .indexSetId(indexSetId)
                .fields(ImmutableSet.<FieldTypeDTO>builder()
                        .add(FieldTypeDTO.create("message", "text"))
                        .add(FieldTypeDTO.create("source", "text"))
                        .add(FieldTypeDTO.create("timestamp", "date"))
                        .add(FieldTypeDTO.create("http_method", "keyword"))
                        .add(FieldTypeDTO.create("http_status", "long"))
                        .addAll(fields)
                        .build())
                .build();
    }

    private IndexFieldTypesDTO createDto(String indexName, Set<FieldTypeDTO> fields) {
        return createDto(indexName, "abc123", fields);
    }

    @Test
    public void saveGetDeleteStream() {
        final IndexFieldTypesDTO newDto1 = createDto("graylog_0", Collections.emptySet());
        final IndexFieldTypesDTO newDto2 = createDto("graylog_1", Collections.emptySet());

        final IndexFieldTypesDTO savedDto1 = dbService.save(newDto1);
        final IndexFieldTypesDTO savedDto2 = dbService.save(newDto2);

        final IndexFieldTypesDTO dto1 = dbService.get(savedDto1.id()).orElse(null);
        assertThat(dto1).as("check that saving the DTO worked").isNotNull();
        assertThat(dto1.id()).isNotBlank();
        assertThat(dto1.indexName()).isEqualTo("graylog_0");
        assertThat(dto1.fields()).containsOnly(
                FieldTypeDTO.create("message", "text"),
                FieldTypeDTO.create("source", "text"),
                FieldTypeDTO.create("timestamp", "date"),
                FieldTypeDTO.create("http_method", "keyword"),
                FieldTypeDTO.create("http_status", "long")
        );

        final IndexFieldTypesDTO dto2 = dbService.get(savedDto2.indexName()).orElse(null);
        assertThat(dto2)
                .as("check that get by index_name works")
                .isNotNull()
                .extracting("indexName")
                .containsOnly("graylog_1");

        assertThat(dbService.findAll().size())
                .as("check that all entries are returned as a stream")
                .isEqualTo(2);

        dbService.delete(dto1.id());
        assertThat(dbService.get(dto1.id())).as("check that delete works").isNotPresent();
    }

    @Test
    public void upsert() {
        final IndexFieldTypesDTO newDto1 = createDto("graylog_0", Collections.emptySet());
        final IndexFieldTypesDTO newDto2 = createDto("graylog_1", Collections.emptySet());

        assertThat(dbService.findAll().size()).isEqualTo(0);

        final IndexFieldTypesDTO upsertedDto1 = dbService.upsert(newDto1).orElse(null);
        final IndexFieldTypesDTO upsertedDto2 = dbService.upsert(newDto2).orElse(null);

        assertThat(upsertedDto1).isNotNull();
        assertThat(upsertedDto2).isNotNull();

        assertThat(upsertedDto1.indexName()).isEqualTo("graylog_0");
        assertThat(upsertedDto2.indexName()).isEqualTo("graylog_1");

        assertThat(dbService.findAll().size()).isEqualTo(2);

        assertThat(dbService.upsert(newDto1)).isNotPresent();
        assertThat(dbService.upsert(newDto2)).isNotPresent();

        assertThat(dbService.findAll().size()).isEqualTo(2);
    }

    @Test
    public void streamForIndexSet() {
        final IndexFieldTypesDTO newDto1 = createDto("graylog_0", "abc", Collections.emptySet());
        final IndexFieldTypesDTO newDto2 = createDto("graylog_1", "xyz", Collections.emptySet());
        final IndexFieldTypesDTO newDto3 = createDto("graylog_2", "xyz", Collections.emptySet());

        final IndexFieldTypesDTO savedDto1 = dbService.save(newDto1);
        final IndexFieldTypesDTO savedDto2 = dbService.save(newDto2);
        final IndexFieldTypesDTO savedDto3 = dbService.save(newDto3);

        assertThat(dbService.findForIndexSet("abc").size()).isEqualTo(1);
        assertThat(dbService.findForIndexSet("xyz").size()).isEqualTo(2);

        assertThat(dbService.findForIndexSet("abc")).first().isEqualTo(savedDto1);
        assertThat(dbService.findForIndexSet("xyz").toArray()).containsExactly(savedDto2, savedDto3);
    }

    @Test
    public void streamForFieldNames() throws Exception {
        dbService.save(createDto("graylog_0", "abc", Collections.emptySet()));
        dbService.save(createDto("graylog_1", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_2", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_3", "xyz", of(
                FieldTypeDTO.create("yolo1", "text")
        )));

        assertThat(dbService.findForFieldNames(of()).size()).isEqualTo(0);
        assertThat(dbService.findForFieldNames(of("message")).size()).isEqualTo(4);
        assertThat(dbService.findForFieldNames(of("message", "yolo_1")).size()).isEqualTo(4);
        assertThat(dbService.findForFieldNames(of("yolo1")).size()).isEqualTo(1);
        assertThat(dbService.findForFieldNames(of("source")).size()).isEqualTo(4);
        assertThat(dbService.findForFieldNames(of("source", "non-existent")).size()).isEqualTo(4);
        assertThat(dbService.findForFieldNames(of("non-existent")).size()).isEqualTo(0);
        assertThat(dbService.findForFieldNames(of("non-existent", "yolo1")).size()).isEqualTo(1);
    }

    @Test
    public void streamForFieldNamesAndIndices() throws Exception {
        dbService.save(createDto("graylog_0", "abc", Collections.emptySet()));
        dbService.save(createDto("graylog_1", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_2", "xyz", Collections.emptySet()));
        dbService.save(createDto("graylog_3", "xyz", of(
                FieldTypeDTO.create("yolo1", "text")
        )));

        assertThat(dbService.findForFieldNamesAndIndices(
                of(),
                of()
        ).size()).isEqualTo(0);

        assertThat(dbService.findForFieldNamesAndIndices(
                of("message"),
                of("graylog_1")
        ).size()).isEqualTo(1);

        assertThat(dbService.findForFieldNamesAndIndices(
                of("message", "yolo1"),
                of("graylog_1", "graylog_3")
        ).size()).isEqualTo(2);

        assertThat(dbService.findForFieldNamesAndIndices(
                of("message", "yolo1"),
                of("graylog_1", "graylog_3", "graylog_0")
        ).size()).isEqualTo(3);
    }
}
