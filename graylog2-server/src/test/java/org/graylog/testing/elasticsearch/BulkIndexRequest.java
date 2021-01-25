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
package org.graylog.testing.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class BulkIndexRequest {
    private final Map<String, List<Map<String, Object>>> requests = new HashMap<>();

    public Map<String, List<Map<String, Object>>> requests() {
        return requests;
    }

    public void addRequest(String index, Map<String, Object> source) {
        this.requests.putIfAbsent(index, new ArrayList<>());
        this.requests.compute(index, (indexName, requests) -> {
            final List<Map<String, Object>> newRequests = firstNonNull(requests, new ArrayList<>());
            newRequests.add(source);
            return newRequests;
        });
    }
}
