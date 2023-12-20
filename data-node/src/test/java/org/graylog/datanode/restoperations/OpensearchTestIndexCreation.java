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
package org.graylog.datanode.restoperations;


import com.github.rholder.retry.RetryException;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;

import jakarta.ws.rs.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class OpensearchTestIndexCreation extends RestOperation {

    public static final String IT_TEST_INDEX = "it_test_index";

    public OpensearchTestIndexCreation(RestOperationParameters waitingRestOperationParameters) {
        super(waitingRestOperationParameters);
    }

    public void createIndex() {
        validatedResponse("/" + IT_TEST_INDEX, HttpMethod.PUT,
                "{\"settings\" : { \"index.number_of_shards\": 1, \"index.number_of_replicas\": 1 }}",
                "Could not create test index",
                r -> r.extract().body().path("acknowledged").equals(true),
                r -> r.extract().body().path("shards_acknowledged").equals(true)
        );
    }

    public List<String> getShardNodes() throws ExecutionException, RetryException {

        final ValidatableResponse response = new OpensearchTestIndexCreationWait(this.parameters).waitForIndexCreation();
        final JsonPath jsonPath = response.extract().body().jsonPath();

        List<Map<String, String>> shards = jsonPath.getList("$");
        assertEquals(shards.size(), 2, "Expecting one primary and one replica shard.");
        return shards.stream()
                .peek(shard -> assertNotEquals("UNASSIGNED", shard.get("state")))
                .map(shard -> shard.get("node"))
                .collect(Collectors.toList());
    }
}
