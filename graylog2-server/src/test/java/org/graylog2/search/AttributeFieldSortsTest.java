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
package org.graylog2.search;

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.graylog2.rest.resources.entities.AttributeSortSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeFieldSortsTest {

    private static final CodecRegistry CODEC_REGISTRY = MongoClientSettings.getDefaultCodecRegistry();

    private String toJson(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, CODEC_REGISTRY).toJson();
    }

    @Test
    void attributeArrayCreatesPipelineToExtractValue() {
        final AttributeSortSpec spec = AttributeFieldSorts.attributeArray(
                "non_identifying_attributes", "host.name");

        assertThat(spec.needsPipeline()).isTrue();
        assertThat(spec.preSortStages()).hasSize(1);
        assertThat(spec.postSortStages()).hasSize(1);

        // The pre-sort stage should use $set with $filter/$arrayElemAt to extract the value
        final String preSortJson = toJson(spec.preSortStages().getFirst());
        assertThat(preSortJson).contains("$set");
        assertThat(preSortJson).contains("non_identifying_attributes");
        assertThat(preSortJson).contains("host.name");
        assertThat(preSortJson).contains("$filter");
        assertThat(preSortJson).contains("$arrayElemAt");

        // The sort field should be the temporary field
        assertThat(spec.sortField()).isEqualTo("_sort_host_name");

        // The post-sort stage should $unset the temporary field
        final String postSortJson = toJson(spec.postSortStages().getFirst());
        assertThat(postSortJson).contains("$unset");
        assertThat(postSortJson).contains("_sort_host_name");
    }

    @Test
    void attributeArrayHandlesDottedKeysInTempFieldName() {
        final AttributeSortSpec spec = AttributeFieldSorts.attributeArray(
                "attrs", "service.version");

        assertThat(spec.sortField()).isEqualTo("_sort_service_version");
    }
}
