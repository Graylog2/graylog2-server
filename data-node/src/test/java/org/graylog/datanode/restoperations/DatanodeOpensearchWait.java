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
import io.restassured.response.ValidatableResponse;

import java.util.concurrent.ExecutionException;

public class DatanodeOpensearchWait extends WaitingRestOperation {

    public DatanodeOpensearchWait(RestOperationParameters waitingRestOperationParameters) {
        super(waitingRestOperationParameters);
    }

    public ValidatableResponse waitForNodesCount(int countOfNodes) throws ExecutionException, RetryException {
        return waitForResponse("/_cluster/health",
                input -> !input.extract().body().path("discovered_cluster_manager").equals(true),
                input -> !input.extract().body().path("status").equals("green"),
                input -> !input.extract().body().path("number_of_nodes").equals(countOfNodes)
        );
    }

}
