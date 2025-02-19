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
package org.graylog.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.rpc.RetryInfo;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;

public class GrpcUtils {
    private GrpcUtils() {
    }

    public static StatusRuntimeException createThrottledStatusRuntimeException() {
        final com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                .setCode(Status.UNAVAILABLE.getCode().value())
                .setMessage("Server is currently operating in THROTTLED mode")
                .addDetails(Any.pack(
                        RetryInfo.newBuilder().setRetryDelay(Duration.newBuilder().setSeconds(30)).build()))
                .build();
        return StatusProto.toStatusRuntimeException(status);
    }
}
