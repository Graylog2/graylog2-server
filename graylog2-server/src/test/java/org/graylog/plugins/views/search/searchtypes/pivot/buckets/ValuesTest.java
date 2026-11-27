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
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    {
        objectMapper.registerSubtypes(Values.class);
    }

    @Test
    void splitsListsByDefault() throws Exception {
        assertThat(Values.builder().field("user_name").build().keepListsWhole()).isFalse();
        assertThat(read("{\"type\":\"values\",\"fields\":[\"user_name\"]}").keepListsWhole()).isFalse();
        assertThat(read("{\"type\":\"values\",\"fields\":[\"user_name\"],\"keep_lists_whole\":null}").keepListsWhole()).isFalse();
    }

    @Test
    void readsAndWritesKeepListsWhole() throws Exception {
        final Values values = read("{\"type\":\"values\",\"fields\":[\"user_name\"],\"keep_lists_whole\":true}");

        assertThat(values.keepListsWhole()).isTrue();
        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(values)).path("keep_lists_whole").asBoolean()).isTrue();
        assertThat(read(objectMapper.writeValueAsString(values))).isEqualTo(values);
    }

    @Test
    void keepsTheOptionWhenTheLimitChanges() {
        assertThat(Values.builder().field("user_name").keepListsWhole(true).build().withLimit(5).keepListsWhole()).isTrue();
    }

    private Values read(String json) throws Exception {
        return (Values) objectMapper.readValue(json, BucketSpec.class);
    }
}
