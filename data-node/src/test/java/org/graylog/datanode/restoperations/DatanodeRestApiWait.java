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

public class DatanodeRestApiWait extends WaitingRestOperation {

    public DatanodeRestApiWait(RestOperationParameters waitingRestOperationParameters) {
        super(waitingRestOperationParameters);
    }

    public ValidatableResponse waitForAvailableStatus() throws ExecutionException, RetryException {
        return waitForResponse("/",
                input -> !input.extract().body().path("opensearch.node.state").equals("AVAILABLE")
        );
    }

    public ValidatableResponse waitForStoppedStatus() throws ExecutionException, RetryException {
        return waitForResponse("/",
                input -> !input.extract().body().path("opensearch.node.state").equals("TERMINATED")
        );
    }

}
