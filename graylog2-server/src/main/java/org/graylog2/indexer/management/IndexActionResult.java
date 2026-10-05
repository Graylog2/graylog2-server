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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Outcome of an action on one index. Bulk requests carry on past failures, so each index reports its own.
 */
public record IndexActionResult(@JsonProperty("index") String index,
                                @JsonProperty("ok") boolean ok,
                                @JsonProperty("message") String message) {

    public static IndexActionResult ok(String index, String message) {
        return new IndexActionResult(index, true, message);
    }

    public static IndexActionResult failed(String index, String message) {
        return new IndexActionResult(index, false, message);
    }

    public record Response(@JsonProperty("results") List<IndexActionResult> results) {
    }
}
