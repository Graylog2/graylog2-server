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

import com.google.common.collect.ImmutableSet;
import org.graylog2.database.filtering.inmemory.InMemoryFilterExpressionParser;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.fieldtypes.utils.FieldTypeDTOsMerger;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.INDEX;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.OVERRIDDEN_INDEX;
import static org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin.PROFILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IndexFieldTypesListServiceTest {

    IndexFieldTypesListService toTest;

    @Mock
    IndexFieldTypesService indexFieldTypesService;
    @Mock
    IndexSetService indexSetService;
    @Mock
    MongoIndexSet.Factory indexSetFactory;
    @Mock
    IndexFieldTypeProfileService profileService;

    @BeforeEach
    void setUp() {
        toTest = new IndexFieldTypesListService(indexFieldTypesService,
                indexSetService,
                indexSetFactory,
                new FieldTypeDTOsMerger(),
                new InMemoryFilterExpressionParser(),
                profileService);
    }

    @Test
    void testReturnsEmptyPageOnWrongIndexId() {
        doReturn(Optional.empty()).when(indexSetService).get("I_do_not_exist!");

        final PageListResponse<IndexSetFieldType> response = toTest.getIndexSetFieldTypesListPage("I_do_not_exist!", "", List.of(), 0, 10, "index_set_id", Sorting.Direction.ASC);

        assertEquals(0, response.total());
        assertTrue(response.elements().isEmpty());

        verifyNoInteractions(indexFieldTypesService);
        verifyNoInteractions(indexSetFactory);
    }

    @Test
    void testReturnsEmptyResultOnWrongIndexIdForAllCall() {
        doReturn(Optional.empty()).when(indexSetService).get("I_do_not_exist!");

        final List<IndexSetFieldType> response = toTest.getIndexSetFieldTypesList("I_do_not_exist!", "", List.of(), "index_set_id", Sorting.Direction.ASC);
        assertTrue(response.isEmpty());

        verifyNoInteractions(indexFieldTypesService);
        verifyNoInteractions(indexSetFactory);
    }

    @Test
    void testReturnsEmptyPageIfCannotCreateIndexSetFromConfig() {
        IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        doReturn(Optional.of(indexSetConfig)).when(indexSetService).get("I_am_strangely_broken!");
        doReturn(new CustomFieldMappings()).when(indexSetConfig).customFieldMappings();
        doReturn(null).when(indexSetFactory).create(indexSetConfig);


        final PageListResponse<IndexSetFieldType> response = toTest.getIndexSetFieldTypesListPage("I_am_strangely_broken!", "", List.of(), 0, 10, "index_set_id", Sorting.Direction.ASC);

        assertEquals(0, response.total());
        assertTrue(response.elements().isEmpty());

        verifyNoInteractions(indexFieldTypesService);
    }

    @Test
    void testReturnsEmptyListIfCannotCreateIndexSetFromConfigForAllCall() {
        IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        doReturn(Optional.of(indexSetConfig)).when(indexSetService).get("I_am_strangely_broken!");
        doReturn(new CustomFieldMappings()).when(indexSetConfig).customFieldMappings();
        doReturn(null).when(indexSetFactory).create(indexSetConfig);


        final List<IndexSetFieldType> response = toTest.getIndexSetFieldTypesList("I_am_strangely_broken!", "", List.of(), "index_set_id", Sorting.Direction.ASC);

        assertTrue(response.isEmpty());

        verifyNoInteractions(indexFieldTypesService);
    }

    @Test
    void testMergesFieldsFromAllSourcesWhenRetrievingPage() {
        IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        doReturn(Optional.of(indexSetConfig)).when(indexSetService).get("I_am_fine!");
        doReturn("profile_id").when(indexSetConfig).fieldTypeProfile();

        final CustomFieldMappings customFieldMappings = new CustomFieldMappings(
                List.of(new CustomFieldMapping("field_1", "ip"))
        );
        doReturn(customFieldMappings).when(indexSetConfig).customFieldMappings();

        final CustomFieldMappings profileMappings = new CustomFieldMappings(
                List.of(new CustomFieldMapping("field_2", "date"))
        );
        final IndexFieldTypeProfile profile = new IndexFieldTypeProfile("profile_id", "profile_name", "profile_descr", profileMappings);
        doReturn(Optional.of(profile)).when(profileService).get("profile_id");

        MongoIndexSet indexSet = mock(MongoIndexSet.class);
        doReturn("graylog_42").when(indexSet).getActiveWriteIndex();
        doReturn("graylog_41").when(indexSet).getNthIndexBeforeActiveIndexSet(1);
        doReturn(indexSet).when(indexSetFactory).create(indexSetConfig);

        ImmutableSet<FieldTypeDTO> deflectorFields = ImmutableSet.of(
                FieldTypeDTO.create("field_1", "long"),
                FieldTypeDTO.create("field_2", "long"),
                FieldTypeDTO.create("field_3", "ip")
        );
        IndexFieldTypesDTO deflectorDTO = IndexFieldTypesDTO.create("nvmd", "graylog_42", deflectorFields);
        doReturn(deflectorDTO)
                .when(indexFieldTypesService)
                .findOneByIndexName("graylog_42");

        ImmutableSet<FieldTypeDTO> previousFields = ImmutableSet.of(
                FieldTypeDTO.create("field_4", "keyword"),
                FieldTypeDTO.create("field_5", "text")
        );
        IndexFieldTypesDTO previousDTO = IndexFieldTypesDTO.create("nvmd", "graylog_42", previousFields);
        doReturn(previousDTO)
                .when(indexFieldTypesService)
                .findOneByIndexName("graylog_41");


        PageListResponse<IndexSetFieldType> response = toTest.getIndexSetFieldTypesListPage("I_am_fine!", "", List.of(), 0, 2, "field_name", Sorting.Direction.ASC);
        assertThat(response.elements())
                .containsExactly(
                        new IndexSetFieldType("field_1", "ip", OVERRIDDEN_INDEX, false),
                        new IndexSetFieldType("field_2", "date", PROFILE, false)
                );
        List<IndexSetFieldType> allResponse = toTest.getIndexSetFieldTypesList("I_am_fine!", "", List.of(), "field_name", Sorting.Direction.ASC);
        assertThat(allResponse)
                .containsExactly(
                        new IndexSetFieldType("field_1", "ip", OVERRIDDEN_INDEX, false),
                        new IndexSetFieldType("field_2", "date", PROFILE, false),
                        new IndexSetFieldType("field_3", "ip", INDEX, false),
                        new IndexSetFieldType("field_4", "string", INDEX, false),
                        new IndexSetFieldType("field_5", "string_fts", INDEX, false)
                );


        PageListResponse<IndexSetFieldType> descendingResponse = toTest.getIndexSetFieldTypesListPage("I_am_fine!", "", List.of(), 0, 2, "field_name", Sorting.Direction.DESC);
        assertThat(descendingResponse.elements())
                .containsExactly(
                        new IndexSetFieldType("field_5", "string_fts", INDEX, false),
                        new IndexSetFieldType("field_4", "string", INDEX, false)
                );

        List<IndexSetFieldType> descendingAllResponse = toTest.getIndexSetFieldTypesList("I_am_fine!", "", List.of(), "field_name", Sorting.Direction.DESC);
        assertThat(descendingAllResponse)
                .containsExactly(
                        new IndexSetFieldType("field_5", "string_fts", INDEX, false),
                        new IndexSetFieldType("field_4", "string", INDEX, false),
                        new IndexSetFieldType("field_3", "ip", INDEX, false),
                        new IndexSetFieldType("field_2", "date", PROFILE, false),
                        new IndexSetFieldType("field_1", "ip", OVERRIDDEN_INDEX, false)
                );


    }

    @Test
    void testFilteringAndQuerying() {
        IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        doReturn(Optional.of(indexSetConfig)).when(indexSetService).get("I_am_fine!");
        final CustomFieldMappings customFieldMappings = new CustomFieldMappings(
                List.of(new CustomFieldMapping("field_custom", "long")) //expected to be filtered out by "is_custom:false" filter
        );
        doReturn(customFieldMappings).when(indexSetConfig).customFieldMappings();
        MongoIndexSet indexSet = mock(MongoIndexSet.class);
        doReturn("graylog_0").when(indexSet).getActiveWriteIndex();
        doReturn(indexSet).when(indexSetFactory).create(indexSetConfig);

        ImmutableSet<FieldTypeDTO> deflectorFields = ImmutableSet.of(
                FieldTypeDTO.create("field_1", "long"), //expected in results
                FieldTypeDTO.create("field_2", "long"), //expected in results
                FieldTypeDTO.create("field_3", "ip"), //expected to be filtered out by "type:long" filter
                FieldTypeDTO.create("aaaa", "long"), //expected to be filtered out by "field" fieldNameQuery
                FieldTypeDTO.create("bbb", "long"), //expected to be filtered out by "field" fieldNameQuery
                FieldTypeDTO.create("ccc", "ip") //expected to be filtered out by "field" fieldNameQuery
        );
        IndexFieldTypesDTO deflectorDTO = IndexFieldTypesDTO.create("nvmd", "graylog_0", deflectorFields);
        doReturn(deflectorDTO)
                .when(indexFieldTypesService)
                .findOneByIndexName("graylog_0");

        PageListResponse<IndexSetFieldType> response = toTest.getIndexSetFieldTypesListPage("I_am_fine!", "field", List.of("type:long", "origin:INDEX"), 0, 50, "field_name", Sorting.Direction.ASC);
        assertThat(response.elements())
                .containsExactly(
                        new IndexSetFieldType("field_1", "long", INDEX, false),
                        new IndexSetFieldType("field_2", "long", INDEX, false)
                );

    }
}
