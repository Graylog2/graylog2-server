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
package org.graylog2.rest.resources.entities;

import org.graylog2.rest.resources.entities.annotations.EntityAttributesAnnotationParser;
import org.graylog2.rest.resources.entities.annotations.FilterOptionDescription;
import org.graylog2.rest.resources.entities.annotations.FrontendAttributeDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EntityAttributesAnnotationParserTest {

    private EntityAttributesAnnotationParser toTest;

    @BeforeEach
    void setUp() {
        toTest = new EntityAttributesAnnotationParser();
    }

    private interface LightlyAnnotated {

        @FrontendAttributeDescription(
                id = "id",
                title = "Id"
        )
        String id();

    }

    @Test
    void testAnnotationProcessingUsesProperDefaults() {
        final List<EntityAttribute> result = toTest.parse(LightlyAnnotated.class);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsOnly(
                        EntityAttribute.builder()
                                .id("id")
                                .title("Id")
                                .type(null)
                                .sortable(true)
                                .filterable(false)
                                .filterOptions(Set.of())
                                .build()
                );
    }

    private interface HeavilyAnnotated {

        @FrontendAttributeDescription(
                id = "id",
                title = "Id",
                type = "long",
                sortable = false,
                filterable = true,
                filterOptions = {
                        @FilterOptionDescription(id = "1", title = "One"),
                        @FilterOptionDescription(id = "2", title = "Two")
                }
        )
        String id();

    }

    @Test
    void testAnnotationProcessingRetrievesAllData() {
        final List<EntityAttribute> result = toTest.parse(HeavilyAnnotated.class);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsOnly(
                        EntityAttribute.builder()
                                .id("id")
                                .title("Id")
                                .type("long")
                                .sortable(false)
                                .filterable(true)
                                .filterOptions(Set.of(
                                        FilterOption.create("1", "One"),
                                        FilterOption.create("2", "Two")
                                ))
                                .build()
                );
    }
}
