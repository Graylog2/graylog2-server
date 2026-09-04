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

import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Field;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeSortSpecTest {

    @Test
    void fieldCreatesSortSpecWithNoPipelineStages() {
        final AttributeSortSpec spec = AttributeSortSpec.field("last_seen");

        assertThat(spec.preSortStages()).isEmpty();
        assertThat(spec.sortField()).isEqualTo("last_seen");
        assertThat(spec.postSortStages()).isEmpty();
        assertThat(spec.needsPipeline()).isFalse();
    }

    @Test
    void needsPipelineReturnsTrueWhenPreSortStagesExist() {
        final List<Bson> preSortStages = List.of(
                Aggregates.set(new Field<>("_sort_temp", new Document("$literal", "x"))));
        final List<Bson> postSortStages = List.of(Aggregates.unset("_sort_temp"));

        final var spec = new AttributeSortSpec(preSortStages, "_sort_temp", postSortStages);

        assertThat(spec.needsPipeline()).isTrue();
    }
}
