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
package org.graylog.plugins.views.search.rest.scriptingapi.response;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ResponseSchemaEntryTest {

    @Test
    void name() {
        Assertions.assertThat(ResponseSchemaEntry.groupBy("http_method").name()).isEqualTo("grouping: http_method");

        Assertions.assertThat(ResponseSchemaEntry.metric("max", ResponseEntryDataType.STRING, "took_ms").name())
                .isEqualTo("metric: max(took_ms)");

        Assertions.assertThat(ResponseSchemaEntry.metric("count", ResponseEntryDataType.STRING, null).name())
                .isEqualTo("metric: count()");
    }
}
