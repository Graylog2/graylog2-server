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
package org.graylog.testing.completebackend;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.time.Instant;
import java.util.Map;

public record WebhookRequest(@JsonDeserialize(converter = LongToInstantConverter.class) Instant timestamp, String method, String url,
                             Map<String, String> headers, String body) {

    public DocumentContext bodyAsJsonPath() {
        return JsonPath.parse(body);
    }

    private static class LongToInstantConverter extends StdConverter<Long, Instant> {
        public Instant convert(final Long value) {
            return Instant.ofEpochMilli(value);
        }
    }
}
