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
package org.graylog.testing.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Assertions for gRPC calls that are expected to fail with a {@link StatusRuntimeException}.
 */
public final class GrpcAssertions {
    private GrpcAssertions() {
    }

    /**
     * Asserts that the call fails with the given status code.
     *
     * @return the thrown exception, for further assertions (e.g. on status details)
     */
    public static StatusRuntimeException assertStatus(ThrowingCallable call, Status.Code code) {
        final StatusRuntimeException e = catchThrowableOfType(StatusRuntimeException.class, call);

        assertThat(e).as("Expected call to fail with status %s", code).isNotNull();
        assertThat(e.getStatus().getCode()).isEqualTo(code);
        return e;
    }

    /**
     * Asserts that the call fails with the given status code and description.
     *
     * @return the thrown exception, for further assertions (e.g. on status details)
     */
    public static StatusRuntimeException assertStatus(ThrowingCallable call, Status.Code code, String description) {
        final StatusRuntimeException e = assertStatus(call, code);

        assertThat(e.getStatus().getDescription()).isEqualTo(description);
        return e;
    }
}
