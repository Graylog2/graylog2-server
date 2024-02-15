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
package org.graylog2.plugin.lookup;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog.plugins.threatintel.whois.ip.WhoisDataAdapter;
import org.graylog.testing.jackson.JacksonSubtypesAssertions;
import org.graylog2.lookup.adapters.HTTPJSONPathDataAdapter;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

class LookupDataAdapterConfigurationTest {
    @Test
    void subtypes() {
        final var objectMapper = new ObjectMapperProvider().get();
        objectMapper.registerSubtypes(
                new NamedType(WhoisDataAdapter.Config.class, WhoisDataAdapter.NAME),
                new NamedType(HTTPJSONPathDataAdapter.Config.class, HTTPJSONPathDataAdapter.NAME)
        );

        final var httpConfig = HTTPJSONPathDataAdapter.Config.builder()
                .type(HTTPJSONPathDataAdapter.NAME)
                .url("http://graylog.local")
                .singleValueJSONPath(".")
                .userAgent("test")
                .build();

        final var whoisConfig = WhoisDataAdapter.Config.builder()
                .type(WhoisDataAdapter.NAME)
                .connectTimeout(1)
                .readTimeout(1)
                .build();

        JacksonSubtypesAssertions.assertThatDto(httpConfig)
                .withObjectMapper(objectMapper)
                .deserializesWhenGivenSupertype(LookupDataAdapterConfiguration.class)
                .doesNotSerializeWithDuplicateFields();
        JacksonSubtypesAssertions.assertThatDto(whoisConfig)
                .withObjectMapper(objectMapper)
                .deserializesWhenGivenSupertype(LookupDataAdapterConfiguration.class)
                .doesNotSerializeWithDuplicateFields();

    }
}
