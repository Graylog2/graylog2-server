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
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeTunableConditionsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void findsInactiveProperties() throws Exception {
        final var schema = mapper.readTree("""
                {"type":"object","properties":{
                  "enabled":{"type":"boolean"},
                  "path":{"type":"string","x-graylog-enabled-when":[{"property":"enabled","in":["true"]}]},
                  "cloud":{"type":["string","null"],"enum":["GCS","ABS",null]},
                  "abs_key":{"type":"string","x-graylog-enabled-when":[{"property":"cloud","in":["ABS"]}]}}}
                """);
        final var value = mapper.readTree("""
                {"enabled":false,"path":"","cloud":null,"abs_key":""}
                """);

        assertThat(RuntimeTunableConditions.inactivePaths(schema, value))
                .containsExactlyInAnyOrder("path", "abs_key");
    }

    @Test
    void activeWhenConditionsMatch() throws Exception {
        final var schema = mapper.readTree("""
                {"type":"object","properties":{"enabled":{"type":"boolean"},
                 "path":{"type":"string","x-graylog-enabled-when":[{"property":"enabled","in":["true"]}]}}}
                """);

        assertThat(RuntimeTunableConditions.inactivePaths(schema, mapper.readTree("{\"enabled\":true,\"path\":\"\"}"))).isEmpty();
    }
}
