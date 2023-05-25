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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RequestedFieldTest {
    @Test
    void testParsing() {
        final RequestedField streamId = RequestedField.parse("streams.id");
        Assertions.assertThat(streamId.name()).isEqualTo("streams");
        Assertions.assertThat(streamId.decorator()).isEqualTo("id");

        final RequestedField streamName = RequestedField.parse("streams.name");
        Assertions.assertThat(streamName.name()).isEqualTo("streams");
        Assertions.assertThat(streamName.decorator()).isEqualTo("name");

        final RequestedField rawField = RequestedField.parse("streams");
        Assertions.assertThat(rawField.name()).isEqualTo("streams");
        Assertions.assertThat(rawField.decorator()).isNull();
    }
}
