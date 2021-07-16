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

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * This class encapsulates a structure responsible for bearing a batch of failures for further processing.
 * Also it verifies whether failures with one batch are all of the same type.
 */
public class FailureBatch {

    public static final FailureBatch EMPTY_PROCESSING_FAILURE_BATCH = FailureBatch.processingFailureBatch(ImmutableList.of());

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

    public static FailureBatch indexingFailureBatch(List<IndexingFailure> failures) {
        return new FailureBatch(failures, IndexingFailure.class);
    }

    public static FailureBatch processingFailureBatch(List<ProcessingFailure> processingFailures) {
        return new FailureBatch(processingFailures, ProcessingFailure.class);
    }

    public static FailureBatch processingFailureBatch(ProcessingFailure processingFailure) {
        return new FailureBatch(ImmutableList.of(processingFailure), ProcessingFailure.class);
    }

    public Collection<? extends Failure> getFailures() {
        return failures;
    }

    public int size() {
        return failures.size();
    }

    public Class<? extends Failure> getFailureClass() {
        return failureClass;
    }
}
