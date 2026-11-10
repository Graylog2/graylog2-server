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
package org.graylog2.database.filtering;

import com.mongodb.client.model.Sorts;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.AttributeSortSpec;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.AttributeFieldSorts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DbSortResolverTest {

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("instance_uid").title("UID").build(),
            EntityAttribute.builder().id("last_seen").title("Last Seen").build(),
            EntityAttribute.builder().id("status").title("Status")
                    .sortSpec(AttributeSortSpec.field("last_seen"))
                    .build(),
            EntityAttribute.builder().id("hostname").title("Hostname")
                    .dbField("non_identifying_attributes")
                    .sortSpec(AttributeFieldSorts.attributeArray("non_identifying_attributes", "host.name"))
                    .build()
    );

    @Test
    void resolveSimpleFieldUsesFieldNameDirectly() {
        final var result = DbSortResolver.resolve(ATTRIBUTES, "instance_uid", SortOrder.ASCENDING);

        assertThat(result.needsPipeline()).isFalse();
        assertThat(result.sort()).isEqualTo(Sorts.ascending("instance_uid"));
        assertThat(result.preSortStages()).isEmpty();
        assertThat(result.postSortStages()).isEmpty();
    }

    @Test
    void resolveFieldRemapUsesSortSpecField() {
        final var result = DbSortResolver.resolve(ATTRIBUTES, "status", SortOrder.DESCENDING);

        assertThat(result.needsPipeline()).isFalse();
        assertThat(result.sort()).isEqualTo(Sorts.descending("last_seen"));
    }

    @Test
    void resolveArrayAttributeReturnsPipelineStages() {
        final var result = DbSortResolver.resolve(ATTRIBUTES, "hostname", SortOrder.ASCENDING);

        assertThat(result.needsPipeline()).isTrue();
        assertThat(result.preSortStages()).hasSize(1);
        assertThat(result.sort()).isEqualTo(Sorts.ascending("_sort_host_name"));
        assertThat(result.postSortStages()).hasSize(1);
    }

    @Test
    void resolveUnknownFieldFallsBackToFieldName() {
        final var result = DbSortResolver.resolve(ATTRIBUTES, "unknown_field", SortOrder.ASCENDING);

        assertThat(result.needsPipeline()).isFalse();
        assertThat(result.sort()).isEqualTo(Sorts.ascending("unknown_field"));
    }

    @Test
    void resolveAttributeWithDbFieldButNoSortSpecUsesDbField() {
        final List<EntityAttribute> attrs = List.of(
                EntityAttribute.builder().id("custom").title("Custom").dbField("actual_field").build()
        );

        final var result = DbSortResolver.resolve(attrs, "custom", SortOrder.DESCENDING);

        assertThat(result.needsPipeline()).isFalse();
        assertThat(result.sort()).isEqualTo(Sorts.descending("actual_field"));
    }
}
