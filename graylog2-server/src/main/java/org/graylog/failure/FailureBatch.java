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
package org.graylog.failure;

import java.util.List;

/**
 * The primary purpose of this class is to guarantee failures
 * within the list to have the same type.
 */
public class FailureBatch {

    private final List<? extends Failure> failures;
    private final Class<? extends Failure> failureClass;

    public FailureBatch(List<? extends Failure> failures,
                        Class<? extends Failure> failureClass) {
        this.failures = failures;
        this.failureClass = failureClass;
        this.failures.forEach(f -> {
            if (!f.getClass().equals(failureClass)) {
                throw new IllegalArgumentException("Not all failures from the list are instances of " + failureClass.getName());
            }
        });
    }

    public List<? extends Failure> getFailures() {
        return failures;
    }

    public Class<? extends Failure> getFailureClass() {
        return failureClass;
    }
}
