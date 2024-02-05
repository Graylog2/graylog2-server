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
package org.graylog2.rest.bulk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BulkOperationResponse(@JsonProperty("successfully_performed") int successfullyPerformed,
                                    @JsonProperty("failures") List<BulkOperationFailure> failures,
                                    @JsonProperty("errors") List<String> errors) {

    public BulkOperationResponse(int successfullyPerformed, List<BulkOperationFailure> failures, List<String> errors) {
        this.successfullyPerformed = successfullyPerformed;
        this.failures = failures;
        this.errors = errors;
    }

    public BulkOperationResponse(final int successfullyPerformed, final List<BulkOperationFailure> failures) {
        this(successfullyPerformed, failures, List.of());
    }

    public BulkOperationResponse(final List<String> errors) {
        this(0, List.of(), errors);
    }
}
