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

import org.graylog2.plugin.Tools;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;


public class FailureBatchTest {

    @Test
    public void indexingFailureBatch_createsBatchOfIndexingFailures() {
        //given
        final IndexingFailure indFailure1 = createIndexingFailure();
        final IndexingFailure indFailure2 = createIndexingFailure();

        // when
        final FailureBatch failureBatch = FailureBatch.indexingFailureBatch(ImmutableList.of(indFailure1, indFailure2));

        // then
        assertThat(failureBatch.size()).isEqualTo(2);
        assertThat(failureBatch.getFailureClass()).isEqualTo(IndexingFailure.class);
        assertThat(failureBatch.getFailures().contains(indFailure1)).isTrue();
        assertThat(failureBatch.getFailures().contains(indFailure2)).isTrue();
    }


    @Test
    public void indexingFailureBatch_collectionCannotBeMutatedAfterCreation() {
        //given
        final IndexingFailure indFailure1 = createIndexingFailure();
        final IndexingFailure indFailure2 = createIndexingFailure();
        final IndexingFailure indFailure3 = createIndexingFailure();

        final List<IndexingFailure> failureList = new ArrayList<>();
        failureList.add(indFailure1);
        failureList.add(indFailure2);

        // when
        final FailureBatch failureBatch = FailureBatch.indexingFailureBatch(failureList);
        failureList.add(indFailure3);

        // then
        assertThat(failureBatch.size()).isEqualTo(2);
        assertThat(failureBatch.getFailureClass()).isEqualTo(IndexingFailure.class);
        assertThat(failureBatch.getFailures().contains(indFailure1)).isTrue();
        assertThat(failureBatch.getFailures().contains(indFailure2)).isTrue();
    }

    @Test
    public void indexingFailureBatch_creationFailsUponMixedTypesOfFailures() {
        assertThatCode(() ->
                FailureBatch.indexingFailureBatch(ImmutableList.of(createIndexingFailure(), createIndexingFailure(), new CustomIndexingFailure())))
                .isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void processingFailureBatch_createsBatchOfProcessingFailures() {
        //given
        final ProcessingFailure prcFailure1 = createProcessingFailure();
        final ProcessingFailure prcFailure2 = createProcessingFailure();

        // when
        final FailureBatch failureBatch = FailureBatch.processingFailureBatch(ImmutableList.of(prcFailure1, prcFailure2));

        // then
        assertThat(failureBatch.size()).isEqualTo(2);
        assertThat(failureBatch.getFailureClass()).isEqualTo(ProcessingFailure.class);
        assertThat(failureBatch.getFailures().contains(prcFailure1)).isTrue();
        assertThat(failureBatch.getFailures().contains(prcFailure2)).isTrue();
    }


    @Test
    public void processingFailureBatch_createsBatchOfOneProcessingFailure() {
        //given
        final ProcessingFailure prcFailure = createProcessingFailure();

        // when
        final FailureBatch failureBatch = FailureBatch.processingFailureBatch(prcFailure);

        // then
        assertThat(failureBatch.size()).isEqualTo(1);
        assertThat(failureBatch.getFailureClass()).isEqualTo(ProcessingFailure.class);
        assertThat(failureBatch.getFailures().contains(prcFailure)).isTrue();
    }

    @Test
    public void containsIndexingFailures_returnsTrueForIndexingFailuresAndFalseForOtherFailures() {
        assertThat(FailureBatch.indexingFailureBatch(ImmutableList.of(createIndexingFailure())).containsIndexingFailures()).isTrue();
        assertThat(FailureBatch.processingFailureBatch(ImmutableList.of(createProcessingFailure())).containsIndexingFailures()).isFalse();
    }

    @Test
    public void containsProcessingFailures_returnsTrueForProcessingFailuresAndFalseForOtherFailures() {
        assertThat(FailureBatch.processingFailureBatch(ImmutableList.of(createProcessingFailure())).containsProcessingFailures()).isTrue();
        assertThat(FailureBatch.indexingFailureBatch(ImmutableList.of(createIndexingFailure())).containsProcessingFailures()).isFalse();
    }

    private IndexingFailure createIndexingFailure() {
        return new IndexingFailure(
                IndexingFailureCause.MappingError, "Mapping Failed", "Cannot cast String to Double",
                Tools.nowUTC(), null, "target-index"
        );
    }

    private ProcessingFailure createProcessingFailure() {
        return new ProcessingFailure(
                ProcessingFailureCause.UNKNOWN, "failure-cause", "failure-details",
                Tools.nowUTC(), null,
                true);
    }
}

class CustomIndexingFailure extends IndexingFailure {
    CustomIndexingFailure() {
        super(IndexingFailureCause.UNKNOWN, "Mapping Failed", "Cannot cast String to Double",
                Tools.nowUTC(), null, "target-index");
    }
}
