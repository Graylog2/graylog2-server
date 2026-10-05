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
package org.graylog2.indexer.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CatIndexTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesARowWhoseColumnsAreAllStrings() throws Exception {
        final CatIndex row = CatIndex.fromJson(objectMapper.readTree("""
                {"index":"graylog_21","health":"green","status":"open","pri":"1","rep":"0",
                 "docs.count":"12345","store.size":"6789012"}"""));

        assertThat(row).isEqualTo(new CatIndex("graylog_21", "green", "open", 1, 0, 12345L, 6789012L));
    }

    @Test
    void closedIndicesHaveNoHealthDocsOrSize() throws Exception {
        final CatIndex row = CatIndex.fromJson(objectMapper.readTree("""
                {"index":"graylog_5","health":null,"status":"close","pri":"1","rep":"0",
                 "docs.count":null,"store.size":null}"""));

        assertThat(row).isEqualTo(new CatIndex("graylog_5", null, "close", 1, 0, null, null));
    }

    @Test
    void unparseableOrMissingNumbersBecomeNull() throws Exception {
        final CatIndex row = CatIndex.fromJson(objectMapper.readTree("""
                {"index":"graylog_20","health":"red","status":"open","pri":"1","docs.count":"","store.size":"n/a"}"""));

        assertThat(row.replicas()).isNull();
        assertThat(row.docsCount()).isNull();
        assertThat(row.storeSizeBytes()).isNull();
    }
}
