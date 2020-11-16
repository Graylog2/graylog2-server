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
package org.graylog.plugins.views.search.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_CHUNK_SIZE;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_FIELDS;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_QUERY;
import static org.graylog.plugins.views.search.export.ExportMessagesCommand.DEFAULT_STREAMS;
import static org.junit.jupiter.api.Assertions.assertAll;

class MessagesRequestTest {
    @Test
    void fillsDefaults() {
        MessagesRequest defaultRequest = MessagesRequest.builder().build();

        assertAll("Should fill every empty field with default",
                () -> assertThat(defaultRequest.timeRange()).isNotNull(),
                () -> assertThat(defaultRequest.queryString()).isEqualTo(DEFAULT_QUERY),
                () -> assertThat(defaultRequest.streams()).isEqualTo(DEFAULT_STREAMS),
                () -> assertThat(defaultRequest.fieldsInOrder()).isEqualTo(DEFAULT_FIELDS),
                () -> assertThat(defaultRequest.chunkSize()).isEqualTo(DEFAULT_CHUNK_SIZE));
    }
}
