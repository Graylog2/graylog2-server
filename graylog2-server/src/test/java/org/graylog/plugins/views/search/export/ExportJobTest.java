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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExportJobTest {
    @Test
    void subtypes() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();

        final SearchTypeExportJob searchTypeJob = SearchTypeExportJob.create(
                "000000000000000000000001",
                "000000000000000000000002",
                "000000000000000000000003",
                ResultFormat.empty());
        final SearchExportJob searchExportJob = SearchExportJob.create(
                "000000000000000000000001",
                "000000000000000000000002",
                ResultFormat.empty());
        final MessagesRequestExportJob messagesRequestExportJob = MessagesRequestExportJob.create(
                "000000000000000000000001",
                MessagesRequest.builder()
                        .limit(10)
                        .timeZone(DateTimeZone.UTC)
                        .build());

        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(searchTypeJob), ExportJob.class))
                .isInstanceOf(SearchTypeExportJob.class);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(searchExportJob), ExportJob.class))
                .isInstanceOf(SearchExportJob.class);
        assertThat(objectMapper.readValue(objectMapper.writeValueAsString(messagesRequestExportJob), ExportJob.class))
                .isInstanceOf(MessagesRequestExportJob.class);
    }
}
