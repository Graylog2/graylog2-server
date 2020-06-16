package org.graylog.testing.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class BulkIndexRequest {
    private final Map<String, List<Map<String, Object>>> requests = new HashMap<>();

    Map<String, List<Map<String, Object>>> requests() {
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
