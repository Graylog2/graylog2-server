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
package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void otherBucketDefaultsToFalseWhenAbsentFromJson() throws Exception {
        final String json = """
                {"type":"values","field":"source","limit":15}
                """;

        final Values values = objectMapper.readValue(json, Values.class);

        assertThat(values.otherBucket()).isFalse();
    }

    @Test
    void otherBucketDefaultsToFalseWhenExplicitlyNull() throws Exception {
        final String json = """
                {"type":"values","field":"source","limit":15,"other_bucket":null}
                """;

        final Values values = objectMapper.readValue(json, Values.class);

        assertThat(values.otherBucket()).isFalse();
    }

    @Test
    void otherBucketIsReadFromJson() throws Exception {
        final String json = """
                {"type":"values","field":"source","limit":15,"other_bucket":true}
                """;

        final Values values = objectMapper.readValue(json, Values.class);

        assertThat(values.otherBucket()).isTrue();
    }

    @Test
    void otherBucketRoundTripsThroughSerialization() throws Exception {
        final Values values = Values.builder().field("source").limit(5).otherBucket(true).build();

        final String json = objectMapper.writeValueAsString(values);
        final Values readBack = objectMapper.readValue(json, Values.class);

        assertThat(json).contains("\"other_bucket\":true");
        assertThat(readBack).isEqualTo(values);
    }

    @Test
    void builderDefaultsOtherBucketToFalse() {
        assertThat(Values.builder().field("source").limit(5).build().otherBucket()).isFalse();
    }
}
