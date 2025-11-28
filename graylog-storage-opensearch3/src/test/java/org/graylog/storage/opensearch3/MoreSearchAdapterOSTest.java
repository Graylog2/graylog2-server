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
package org.graylog.storage.opensearch3;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.PlainJsonSerializable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

class MoreSearchAdapterOSTest {
    private static final String FIELD = "field";
    private static final JsonData VALUE = JsonData.of("100");

    @Test
    void testBuildExtraFilter() {
        verifyFilter("<=100", Query.builder().range(range -> range.field(FIELD).lte(VALUE)).build());
        verifyFilter(">=100", Query.builder().range(range -> range.field(FIELD).gte(VALUE)).build());
        verifyFilter("<100", Query.builder().range(range -> range.field(FIELD).lt(VALUE)).build());
        verifyFilter(">100", Query.builder().range(range -> range.field(FIELD).gt(VALUE)).build());
        verifyFilter("100", Query.builder().multiMatch(mm -> mm.query("100").fields(FIELD)).build());
    }

    private static void verifyFilter(String value, Query expected) {
        Query generatedQuery = MoreSearchAdapterOS.buildExtraFilter(FIELD, value);
        Assertions.assertThat(generatedQuery)
                .extracting(PlainJsonSerializable::toJsonString)
                .isEqualTo(expected.toJsonString());
    }
}
