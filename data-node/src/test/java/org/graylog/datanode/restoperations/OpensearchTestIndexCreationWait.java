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

public class OpensearchTestIndexCreationWait extends WaitingRestOperation {

    public OpensearchTestIndexCreationWait(RestOperationParameters waitingRestOperationParameters) {
        super(waitingRestOperationParameters);
    }

    public ValidatableResponse waitForIndexCreation() throws ExecutionException, RetryException {
        return waitForResponse("/_cat/shards/" + OpensearchTestIndexCreation.IT_TEST_INDEX + "?h=node,state&format=json",
                input -> input.extract().body().jsonPath().getList("state").size() != 2,
                input -> input.extract().body().jsonPath().getList("state").stream().anyMatch(s -> !s.equals("STARTED"))
        );
    }

}
