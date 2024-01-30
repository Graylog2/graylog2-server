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
package org.graylog2.indexer.fieldtypes.utils;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.rest.resources.system.indexer.responses.FieldTypeOrigin;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldTypeDTOsMergerTest {

    private FieldTypeDTOsMerger toTest;

    @BeforeEach
    void setUp() {
        toTest = new FieldTypeDTOsMerger();
    }

    @Test
    void fieldDTOsFromNewerIndexOverrideFieldDTOsFromOlderIndex() {
        final Collection<IndexSetFieldType> merged = toTest.merge(
                List.of(
                        FieldTypeDTO.create("changed_field", "long")
                ),
                List.of(
                        FieldTypeDTO.create("changed_field", "date")
                ),
                new CustomFieldMappings(),
                new IndexFieldTypeProfile("id", "name", "descr", new CustomFieldMappings())
        );

        assertThat(merged)
                .isNotNull()
                .hasSize(1)
                .contains(new IndexSetFieldType("changed_field", "long", FieldTypeOrigin.INDEX, false));
    }

    @Test
    void customMappingsOverrideEverything() {
        final Collection<IndexSetFieldType> merged = toTest.merge(
                List.of(
                        FieldTypeDTO.create("changed_field", "long")
                ),
                List.of(
                        FieldTypeDTO.create("changed_field", "date")
                ),
                new CustomFieldMappings(
                        List.of(new CustomFieldMapping("changed_field", "ip"))
                ),
                new IndexFieldTypeProfile("id", "name", "descr", new CustomFieldMappings(
                        List.of(new CustomFieldMapping("changed_field", "double"))
                ))
        );

        assertThat(merged)
                .isNotNull()
                .hasSize(1)
                .contains(new IndexSetFieldType("changed_field", "ip", FieldTypeOrigin.OVERRIDDEN_PROFILE, false));
    }

    @Test
    void complexMergeScenario() {
        final Collection<IndexSetFieldType> merged = toTest.merge(
                List.of(
                        FieldTypeDTO.create("unique_in_new", "long"),
                        FieldTypeDTO.create("present_everywhere", "long"),
                        FieldTypeDTO.create("present_new_and_old", "long"),
                        FieldTypeDTO.create("present_custom_and_new", "long"),
                        FieldTypeDTO.create("present_profile_and_new", "long")
                ),
                List.of(
                        FieldTypeDTO.create("unique_in_old", "date"),
                        FieldTypeDTO.create("present_everywhere", "date"),
                        FieldTypeDTO.create("present_new_and_old", "date"),
                        FieldTypeDTO.create("present_custom_and_old", "date")
                ),
                new CustomFieldMappings(
                        List.of(
                                new CustomFieldMapping("unique_in_custom", "ip"),
                                new CustomFieldMapping("present_everywhere", "ip"),
                                new CustomFieldMapping("present_custom_and_new", "ip"),
                                new CustomFieldMapping("present_custom_and_old", "ip"),
                                new CustomFieldMapping("present_custom_and_profile", "ip")
                        )
                ),
                new IndexFieldTypeProfile("id", "name", "descr", new CustomFieldMappings(
                        List.of(
                                new CustomFieldMapping("present_everywhere", "double"),
                                new CustomFieldMapping("unique_in_profile", "double"),
                                new CustomFieldMapping("present_custom_and_profile", "double"),
                                new CustomFieldMapping("present_profile_and_new", "double")
                        )
                ))
        );

        assertThat(merged)
                .isNotNull()
                .hasSize(10)
                .contains(new IndexSetFieldType("present_everywhere", "ip", FieldTypeOrigin.OVERRIDDEN_PROFILE, false))
                .contains(new IndexSetFieldType("unique_in_custom", "ip", FieldTypeOrigin.OVERRIDDEN_INDEX, false))
                .contains(new IndexSetFieldType("present_custom_and_new", "ip", FieldTypeOrigin.OVERRIDDEN_INDEX, false))
                .contains(new IndexSetFieldType("present_custom_and_old", "ip", FieldTypeOrigin.OVERRIDDEN_INDEX, false))
                .contains(new IndexSetFieldType("present_custom_and_profile", "ip", FieldTypeOrigin.OVERRIDDEN_PROFILE, false))
                .contains(new IndexSetFieldType("unique_in_profile", "double", FieldTypeOrigin.PROFILE, false))
                .contains(new IndexSetFieldType("present_profile_and_new", "double", FieldTypeOrigin.PROFILE, false))
                .contains(new IndexSetFieldType("unique_in_new", "long", FieldTypeOrigin.INDEX, false))
                .contains(new IndexSetFieldType("present_new_and_old", "long", FieldTypeOrigin.INDEX, false))
                .contains(new IndexSetFieldType("unique_in_old", "date", FieldTypeOrigin.INDEX, false));

    }
}
