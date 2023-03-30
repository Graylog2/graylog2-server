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
package org.graylog2.rest.resources.system.contentpacks.titles.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

class EntitiesTitleResponseTest {

    @Test
    void testMergeWithEmptyOrNullResponse() {
        EntitiesTitleResponse response = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type", "title")), Set.of());

        assertSame(response, response.merge(new EntitiesTitleResponse(Set.of(), Set.of())));
        assertSame(response, response.merge(new EntitiesTitleResponse(null, Set.of())));
        assertSame(response, new EntitiesTitleResponse(Set.of(), Set.of()).merge(response));
    }


    @Test
    void testMergeWithNonEmptyResponse() {
        EntitiesTitleResponse response1 = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type1", "title")), Set.of());
        EntitiesTitleResponse response2 = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type2", "title")), Set.of());

        final EntitiesTitleResponse merged = response1.merge(response2);
        assertThat(merged.entities()).isEqualTo(
                Set.of(
                        new EntityTitleResponse("id", "type1", "title"),
                        new EntityTitleResponse("id", "type2", "title")
                )
        );
    }

    @Test
    void testDuplicatesAreEliminatedOnMerge() {
        EntitiesTitleResponse response1 = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type", "title")), Set.of());
        EntitiesTitleResponse response2 = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type", "title")), Set.of());

        final EntitiesTitleResponse merged = response1.merge(response2);
        assertThat(merged.entities()).isEqualTo(
                Set.of(
                        new EntityTitleResponse("id", "type", "title")
                )
        );
    }

    @Test
    void testMergeWithNonPermittedSection() {
        EntitiesTitleResponse response1 = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type1", "title")), Set.of("secret1"));
        EntitiesTitleResponse response2 = new EntitiesTitleResponse(Set.of(new EntityTitleResponse("id", "type2", "title")), Set.of("secret1", "secret2"));

        final EntitiesTitleResponse merged = response1.merge(response2);
        assertThat(merged.entities()).isEqualTo(
                Set.of(
                        new EntityTitleResponse("id", "type1", "title"),
                        new EntityTitleResponse("id", "type2", "title")
                )
        );
        assertThat(merged.notPermitted())
                .containsAll(List.of("secret1", "secret2"));

    }

}
