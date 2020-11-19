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
package org.graylog2.indexer.ranges;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MongoIndexRangeTest {
    @Test
    public void testCreate() throws Exception {
        String indexName = "test";
        DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2015, 2, 1, 0, 0, DateTimeZone.UTC);
        DateTime calculatedAt = new DateTime(2015, 2, 1, 0, 0, DateTimeZone.UTC);
        int calculationDuration = 42;
        MongoIndexRange indexRange = MongoIndexRange.create(indexName, begin, end, calculatedAt, calculationDuration);

        assertThat(indexRange.indexName()).isEqualTo(indexName);
        assertThat(indexRange.begin()).isEqualTo(begin);
        assertThat(indexRange.end()).isEqualTo(end);
        assertThat(indexRange.calculatedAt()).isEqualTo(calculatedAt);
        assertThat(indexRange.calculationDuration()).isEqualTo(calculationDuration);
    }

    @Test
    public void testJsonMapping() throws Exception {
        String indexName = "test";
        DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2015, 2, 1, 0, 0, DateTimeZone.UTC);
        DateTime calculatedAt = new DateTime(2015, 2, 1, 0, 0, DateTimeZone.UTC);
        int calculationDuration = 42;
        MongoIndexRange indexRange = MongoIndexRange.create(indexName, begin, end, calculatedAt, calculationDuration);

        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        String json = objectMapper.writeValueAsString(indexRange);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);

        assertThat((String) JsonPath.read(document, "$." + MongoIndexRange.FIELD_INDEX_NAME)).isEqualTo(indexName);
        assertThat((long) JsonPath.read(document, "$." + MongoIndexRange.FIELD_BEGIN)).isEqualTo(begin.getMillis());
        assertThat((long) JsonPath.read(document, "$." + MongoIndexRange.FIELD_END)).isEqualTo(end.getMillis());
        assertThat((long) JsonPath.read(document, "$." + MongoIndexRange.FIELD_CALCULATED_AT)).isEqualTo(calculatedAt.getMillis());
        assertThat((int) JsonPath.read(document, "$." + MongoIndexRange.FIELD_TOOK_MS)).isEqualTo(calculationDuration);
    }
}