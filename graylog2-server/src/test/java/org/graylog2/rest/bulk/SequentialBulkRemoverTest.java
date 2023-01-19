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
package org.graylog2.rest.bulk;

import com.mongodb.MongoException;
import org.graylog2.database.NotFoundException;
import org.graylog2.rest.bulk.model.BulkDeleteRequest;
import org.graylog2.rest.bulk.model.BulkDeleteResponse;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.bulk.SequentialBulkRemover.NO_ENTITY_IDS_FAILURE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class SequentialBulkRemoverTest {

    private SequentialBulkRemover<Object> toTest;
    @Mock
    private SingleEntityRemover<Object> singleEntityRemover;
    private final Object context = new Object();

    @BeforeEach
    void setUp() {
        toTest = new SequentialBulkRemover<>(singleEntityRemover);
    }

    @Test
    void returnsProperFailureMsgOnNullEntityIdsList() {
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(null), context);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(0);
        assertThat(bulkDeleteResponse.failures()).containsOnly(NO_ENTITY_IDS_FAILURE);
        verifyNoInteractions(singleEntityRemover);
    }

    @Test
    void returnsProperFailureMsgOnEmptyEntityIdsList() {
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of()), context);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(0);
        assertThat(bulkDeleteResponse.failures()).containsOnly(NO_ENTITY_IDS_FAILURE);
        verifyNoInteractions(singleEntityRemover);
    }

    @Test
    void returnsProperResponseOnSuccessfulBulkRemoval() throws Exception {
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("1", "2", "3")), context);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(3);
        assertThat(bulkDeleteResponse.failures()).isEmpty();
        verify(singleEntityRemover).remove("1", context);
        verify(singleEntityRemover).remove("2", context);
        verify(singleEntityRemover).remove("3", context);
        verifyNoMoreInteractions(singleEntityRemover);
    }

    @Test
    void returnsProperResponseOnFailedBulkRemoval() throws Exception {
        doThrow(new NotFoundException("!?!?")).when(singleEntityRemover).remove(any(), eq(context));
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("no", "good", "ids")), context);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(0);
        assertThat(bulkDeleteResponse.failures())
                .hasSize(3)
                .containsExactly(
                        new BulkOperationFailure("no", "!?!?"),
                        new BulkOperationFailure("good", "!?!?"),
                        new BulkOperationFailure("ids", "!?!?")
                );
        verify(singleEntityRemover).remove("no", context);
        verify(singleEntityRemover).remove("good", context);
        verify(singleEntityRemover).remove("ids", context);
        verifyNoMoreInteractions(singleEntityRemover);
    }

    @Test
    void returnsProperResponseOnPartiallySuccessfulBulkRemoval() throws Exception {
        doThrow(new MongoException("MongoDB is striking against increasing retirement age")).when(singleEntityRemover).remove(eq("1"), eq(context));
        final BulkDeleteResponse bulkDeleteResponse = toTest.bulkDelete(new BulkDeleteRequest(List.of("1", "2", "3")), context);
        assertThat(bulkDeleteResponse.successfullyRemoved()).isEqualTo(2);
        assertThat(bulkDeleteResponse.failures())
                .hasSize(1)
                .containsExactly(
                        new BulkOperationFailure("1", "MongoDB is striking against increasing retirement age")
                );
        verify(singleEntityRemover).remove("1", context);
        verify(singleEntityRemover).remove("2", context);
        verify(singleEntityRemover).remove("3", context);
        verifyNoMoreInteractions(singleEntityRemover);
    }
}
