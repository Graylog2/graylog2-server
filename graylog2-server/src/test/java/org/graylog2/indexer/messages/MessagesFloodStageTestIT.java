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
package org.graylog2.indexer.messages;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.failure.FailureSubmissionService;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public abstract class MessagesFloodStageTestIT extends ElasticsearchBaseTest {
    private static final String INDEX_NAME = "messages_it_deflector";

    protected Messages messages;

    protected static final IndexSet indexSet = new MessagesTestIndexSet();

    protected MessagesAdapter createMessagesAdapter() {
        return searchServer().adapters().messagesAdapter();
    }

    private final FailureSubmissionService failureSubmissionService = mock(FailureSubmissionService.class);

    @Before
    public void setUp() throws Exception {
        client().deleteIndices(INDEX_NAME);
        client().createIndex(INDEX_NAME);
        client().waitForGreenStatus(INDEX_NAME);
        messages = new Messages(mock(TrafficAccounting.class), createMessagesAdapter(), mock(ProcessingStatusRecorder.class),
                failureSubmissionService);
    }

    @After
    public void tearDown() {
        client().resetClusterBlock();
        client().cleanUp();
    }

    protected long messageCount(String indexName) {
        searchServer().client().refreshNode();
        return searchServer().adapters().countsAdapter().totalCount(Collections.singletonList(indexName));
    }


    @Test
    public void retryIndexingMessagesDuringFloodStage() throws Exception {
        triggerFloodStage(INDEX_NAME);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicBoolean succeeded = new AtomicBoolean(false);
        final List<Map.Entry<IndexSet, Message>> messageBatch = createMessageBatch(1024, 50);

        final Future<List<String>> result = background(() -> this.messages.bulkIndex(messageBatch, createIndexingListener(countDownLatch, succeeded)));

        countDownLatch.await();

        resetFloodStage(INDEX_NAME);

        final List<String> failedItems = result.get(3, TimeUnit.MINUTES);
        assertThat(failedItems).isEmpty();

        client().refreshNode();

        assertThat(messageCount(INDEX_NAME)).isEqualTo(50);
        assertThat(succeeded.get()).isTrue();
    }

    private Messages.IndexingListener createIndexingListener(CountDownLatch retryLatch, AtomicBoolean successionFlag) {
        return new Messages.IndexingListener() {
            @Override
            public void onRetry(long attemptNumber) {
                retryLatch.countDown();
            }

            @Override
            public void onSuccess(long delaySinceFirstAttempt) {
                if (retryLatch.getCount() > 0) {
                    retryLatch.countDown();
                }
                successionFlag.set(true);
            }
        };
    }

    private Future<List<String>> background(Callable<List<String>> task) {
        final ExecutorService executor = Executors.newFixedThreadPool(1,
                new ThreadFactoryBuilder().setNameFormat("messages-it-%d").build());

        return executor.submit(task);
    }

    private void triggerFloodStage(String index) {
        client().putSetting("cluster.routing.allocation.disk.watermark.low", "0%");
        client().putSetting("cluster.routing.allocation.disk.watermark.high", "0%");
        client().putSetting("cluster.routing.allocation.disk.watermark.flood_stage", "0%");

        client().waitForIndexBlock(index);
    }

    private void resetFloodStage(String index) {
        client().putSetting("cluster.routing.allocation.disk.watermark.flood_stage", "95%");
        client().putSetting("cluster.routing.allocation.disk.watermark.high", "90%");
        client().putSetting("cluster.routing.allocation.disk.watermark.low", "85%");

        client().resetIndexBlock(index);
        client().resetClusterBlock();
    }

    private DateTime now() {
        return DateTime.now(DateTimeZone.UTC);
    }

    private ArrayList<Map.Entry<IndexSet, Message>> createMessageBatch(int size, int count) {
        final ArrayList<Map.Entry<IndexSet, Message>> messageList = new ArrayList<>();

        final String message = "A".repeat(size);
        for (int i = 0; i < count; i++) {
            messageList.add(Maps.immutableEntry(indexSet, new Message(i + message, "source", now())));
        }
        return messageList;
    }
}
