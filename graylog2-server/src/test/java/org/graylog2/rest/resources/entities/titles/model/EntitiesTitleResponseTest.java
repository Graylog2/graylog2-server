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
package org.graylog2.rest.resources.entities.titles.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

class EntitiesTitleResponseTest {

    @Test
    void testMergeWithEmptyOrNullResponse() {
        EntitiesTitleResponse response = new EntitiesTitleResponse(List.of(new EntityTitleResponse("id", "type", "title")));

        assertSame(response, response.merge(new EntitiesTitleResponse(List.of())));
        assertSame(response, response.merge(new EntitiesTitleResponse(null)));
        assertSame(response, new EntitiesTitleResponse(List.of()).merge(response));
    }


    @Test
    void testMergeWithNonEmptyResponse() {
        EntitiesTitleResponse response1 = new EntitiesTitleResponse(List.of(new EntityTitleResponse("id", "type1", "title")));
        EntitiesTitleResponse response2 = new EntitiesTitleResponse(List.of(new EntityTitleResponse("id", "type2", "title")));

        final EntitiesTitleResponse merged = response1.merge(response2);
        assertThat(merged.entities()).isEqualTo(
                List.of(
                        new EntityTitleResponse("id", "type1", "title"),
                        new EntityTitleResponse("id", "type2", "title")
                )
        );
    }

    @Test
    void testDuplicatesAreEliminatedOnMerge() {
        EntitiesTitleResponse response1 = new EntitiesTitleResponse(List.of(new EntityTitleResponse("id", "type", "title")));
        EntitiesTitleResponse response2 = new EntitiesTitleResponse(List.of(new EntityTitleResponse("id", "type", "title")));

        final EntitiesTitleResponse merged = response1.merge(response2);
        assertThat(merged.entities()).isEqualTo(
                List.of(
                        new EntityTitleResponse("id", "type", "title")
                )
        );
    }

}
