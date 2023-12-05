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


import javax.ws.rs.HttpMethod;

public class DatanodeStatusChangeOperation extends RestOperation {

    public DatanodeStatusChangeOperation(RestOperationParameters waitingRestOperationParameters) {
        super(waitingRestOperationParameters);
    }

    public void triggerNodeRemoval() {
        validatedResponse("/management", HttpMethod.DELETE, null,
                "Could not trigger node removal",
                r -> r.extract().statusCode() < 300
        );
    }

    public void triggerNodeStop() {
        validatedResponse("/management/stop", HttpMethod.POST, "",
                "Could not trigger node stop",
                r -> r.extract().statusCode() < 300
        );
    }

    public void triggerNodeStart() {
        validatedResponse("/management/start", HttpMethod.POST, "",
                "Could not trigger node start",
                r -> r.extract().statusCode() < 300
        );
    }

}
