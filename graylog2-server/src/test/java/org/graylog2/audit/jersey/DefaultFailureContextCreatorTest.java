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
package org.graylog2.audit.jersey;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultFailureContextCreatorTest {

    @Test
    void createsProperContext() {
        final Map<String, Object> expected = Map.of("path_params", Map.of("streamId", List.of("00000000000000042")));
        DefaultFailureContextCreator toTest = new DefaultFailureContextCreator();
        assertThat(toTest.create("streamId", "00000000000000042")).isEqualTo(expected);
    }
}
