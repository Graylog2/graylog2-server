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

    private final List<? extends Failure> failures;
    private final Class<? extends Failure> failureClass;

    private FailureBatch(List<? extends Failure> failures,
                        Class<? extends Failure> failureClass) {
        this.failures = ImmutableList.copyOf(failures);
        this.failureClass = failureClass;
        this.failures.forEach(f -> {
            if (!f.getClass().equals(failureClass)) {
                throw new IllegalArgumentException("Not all failures from the list are instances of " + failureClass.getName());
            }
        });
    }

    /**
     * @param indexingFailures a list of indexing failures to be included in the batch.
     * @return a batch of indexing failures.
     */
    public static FailureBatch indexingFailureBatch(List<IndexingFailure> indexingFailures) {
        return new FailureBatch(indexingFailures, IndexingFailure.class);
    }

    /**
     * @param processingFailures a list of processing failures to be included in the batch.
     * @return a batch of processing failures.
     */
    public static FailureBatch processingFailureBatch(List<ProcessingFailure> processingFailures) {
        return new FailureBatch(processingFailures, ProcessingFailure.class);
    }

    /**
     * Creates a batch containing only one processing failure. A shortcut for
     * FailureBatch.processingFailureBatch(ImmutableList.of(processingFailure));
     */
    public static FailureBatch processingFailureBatch(ProcessingFailure processingFailure) {
        return processingFailureBatch(ImmutableList.of(processingFailure));
    }

    /**
     * @return an underlying immutable collection contains the failures.
     */
    public Collection<? extends Failure> getFailures() {
        return failures;
    }

    /**
     * @return a number of failures within the batch.
     */
    public int size() {
        return failures.size();
    }

    /**
     * @return a class of failures within the batch.
     */
    public Class<? extends Failure> getFailureClass() {
        return failureClass;
    }
}
