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
        final Collection<FieldTypeDTO> merged = toTest.merge(
                List.of(
                        FieldTypeDTO.create("changed_field", "long")
                ),
                List.of(
                        FieldTypeDTO.create("changed_field", "text")
                ),
                new CustomFieldMappings(),
                new IndexFieldTypeProfile("id", "name", "descr", new CustomFieldMappings())
        );

        assertThat(merged)
                .isNotNull()
                .hasSize(1)
                .contains(FieldTypeDTO.create("changed_field", "long"));
    }

    @Test
    void customMappingsOverrideEverything() {
        final Collection<FieldTypeDTO> merged = toTest.merge(
                List.of(
                        FieldTypeDTO.create("changed_field", "long")
                ),
                List.of(
                        FieldTypeDTO.create("changed_field", "text")
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
                .contains(FieldTypeDTO.create("changed_field", "ip"));
    }

    @Test
    void complexMergeScenario() {
        final Collection<FieldTypeDTO> merged = toTest.merge(
                List.of(
                        FieldTypeDTO.create("unique_in_new", "long"),
                        FieldTypeDTO.create("present_everywhere", "long"),
                        FieldTypeDTO.create("present_new_and_old", "long"),
                        FieldTypeDTO.create("present_custom_and_new", "long"),
                        FieldTypeDTO.create("present_profile_and_new", "long")
                ),
                List.of(
                        FieldTypeDTO.create("unique_in_old", "text"),
                        FieldTypeDTO.create("present_everywhere", "text"),
                        FieldTypeDTO.create("present_new_and_old", "text"),
                        FieldTypeDTO.create("present_custom_and_old", "text")
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
                .contains(FieldTypeDTO.create("present_everywhere", "ip"))
                .contains(FieldTypeDTO.create("unique_in_custom", "ip"))
                .contains(FieldTypeDTO.create("present_custom_and_new", "ip"))
                .contains(FieldTypeDTO.create("present_custom_and_old", "ip"))
                .contains(FieldTypeDTO.create("present_custom_and_profile", "ip"))
                .contains(FieldTypeDTO.create("unique_in_profile", "double"))
                .contains(FieldTypeDTO.create("present_profile_and_new", "double"))
                .contains(FieldTypeDTO.create("unique_in_new", "long"))
                .contains(FieldTypeDTO.create("present_new_and_old", "long"))
                .contains(FieldTypeDTO.create("unique_in_old", "text"));

    }
}
