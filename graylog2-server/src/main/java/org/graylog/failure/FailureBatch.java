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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * A structure bearing a batch of failures. It guarantees all failures
 * to be instances of the same class
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
     * @param indexingFailures a list of indexing failures to include in this batch
     * @return a batch of indexing failures
     * @throws IllegalArgumentException if not all failures are instances of {@link IndexingFailure}
     */
    public static FailureBatch indexingFailureBatch(List<IndexingFailure> indexingFailures) {
        return new FailureBatch(indexingFailures, IndexingFailure.class);
    }

    /**
     * @param processingFailures a list of processing failures to include in this batch
     * @return a batch of processing failures
     * @throws IllegalArgumentException if not all failures are instances of {@link ProcessingFailure}
     */
    public static FailureBatch processingFailureBatch(List<ProcessingFailure> processingFailures) {
        return new FailureBatch(processingFailures, ProcessingFailure.class);
    }

    /**
     * Creates a batch containing only one processing failure
     * @param processingFailure a processing failure to include in this batch
     * @return a batch with the processing failure
     * @throws IllegalArgumentException if the failure is not instances of {@link ProcessingFailure}
     */
    public static FailureBatch processingFailureBatch(ProcessingFailure processingFailure) {
        return processingFailureBatch(ImmutableList.of(processingFailure));
    }

    /**
     * @return a list of failures within the batch. The returned collection is immutable.
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

    /**
     * @return true if the batch contains indexing failures.
     */
    public boolean containsIndexingFailures() {
        return getFailureClass().equals(IndexingFailure.class);
    }

    /**
     * @return true if the batch contains processing failures.
     */
    public boolean containsProcessingFailures() {
        return getFailureClass().equals(ProcessingFailure.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final FailureBatch that = (FailureBatch) o;
        return Objects.equal(failures, that.failures) && Objects.equal(failureClass, that.failureClass);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(failures, failureClass);
    }
}
