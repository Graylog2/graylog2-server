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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import joptsimple.internal.Strings;
import org.graylog.testing.elasticsearch.ElasticsearchBaseTest;
import org.graylog2.indexer.IndexFailure;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.Message;
import org.graylog2.system.processing.ProcessingStatusRecorder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public abstract class MessagesIT extends ElasticsearchBaseTest {
    private static final String INDEX_NAME = "messages_it_deflector";

    protected Messages messages;

    private static final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
            .id("index-set-1")
            .title("Index set 1")
            .description("For testing")
            .indexPrefix("messages_it")
            .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
            .shards(1)
            .replicas(0)
            .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
            .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
            .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
            .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
            .indexAnalyzer("standard")
            .indexTemplateName("template-1")
            .indexOptimizationMaxNumSegments(1)
            .indexOptimizationDisabled(false)
            .build();
    private static final IndexSet indexSet = new TestIndexSet(indexSetConfig);

    protected abstract MessagesAdapter createMessagesAdapter(MetricRegistry metricRegistry);

    @Before
    public void setUp() throws Exception {
        client().deleteIndices(INDEX_NAME);
        client().createIndex(INDEX_NAME);
        client().waitForGreenStatus(INDEX_NAME);
        final MetricRegistry metricRegistry = new MetricRegistry();
        messages = new Messages(mock(TrafficAccounting.class), createMessagesAdapter(metricRegistry), mock(ProcessingStatusRecorder.class));
    }

    @After
    public void tearDown() {
        client().deleteIndices(INDEX_NAME);
    }

    protected abstract boolean indexMessage(String index, Map<String, Object> source, @SuppressWarnings("SameParameterValue") String id);

    @Test
    public void getRetrievesPreviouslyStoredMessage() throws Exception {
        final String index = UUID.randomUUID().toString();
        client().createIndex(index);

        final Map<String, Object> source = new HashMap<>();
        source.put("message", "This is my message");
        source.put("source", "logsender");
        source.put("timestamp", "2017-04-13 15:29:00.000");

        assertThat(indexMessage(index, source, "1")).isTrue();

        final ResultMessage resultMessage = messages.get("1", index);
        final Message message = resultMessage.getMessage();

        assertThat(message).isNotNull();
        assertThat(message.getMessage()).isEqualTo("This is my message");
        assertThat(message.getSource()).isEqualTo("logsender");
        assertThat(message.getTimestamp()).isEqualTo(DateTime.parse("2017-04-13T15:29:00.000Z"));
    }

    @Test
    public void analyzingFieldReturnsTokens() throws IOException {
        final String randomIndex = client().createRandomIndex("analyze-");
        final List<String> terms = messages.analyze("The quick brown fox jumps over the lazy dog", randomIndex, "standard");

        assertThat(terms).containsExactlyInAnyOrder("the", "quick", "brown", "fox", "jumps", "over", "the", "lazy", "dog");
    }

    @Test
    public void testIfTooLargeBatchesGetSplitUp() throws Exception {
        // This test assumes that ES is configured with bulk_max_body_size to 100MB
        // Check if we can index about 300MB of messages (once the large batch gets split up)
        final int MESSAGECOUNT = 101;
        // Each Message is about 1 MB
        final List<Map.Entry<IndexSet, Message>> largeMessageBatch = createMessageBatch(1024 * 1024, MESSAGECOUNT);
        final List<String> failedItems = this.messages.bulkIndex(largeMessageBatch);

        assertThat(failedItems).isEmpty();

        Thread.sleep(2000); // wait for ES to finish indexing

        assertThat(messageCount(INDEX_NAME)).isEqualTo(MESSAGECOUNT);
    }

    protected abstract long messageCount(String indexName);

    @Test
    public void unevenTooLargeBatchesGetSplitUp() throws Exception {
        final int MESSAGECOUNT = 100;
        final int LARGE_MESSAGECOUNT = 20;
        final List<Map.Entry<IndexSet, Message>> messageBatch = createMessageBatch(1024, MESSAGECOUNT);
        messageBatch.addAll(createMessageBatch(1024 * 1024 * 5, LARGE_MESSAGECOUNT));
        final List<String> failedItems = this.messages.bulkIndex(messageBatch);

        assertThat(failedItems).isEmpty();

        client().refreshNode(); // wait for ES to finish indexing

        assertThat(messageCount(INDEX_NAME)).isEqualTo(MESSAGECOUNT + LARGE_MESSAGECOUNT);
    }

    @Test
    public void conflictingFieldTypesErrorAreReported() throws Exception {
        final String fieldName = "_ourcustomfield";
        final Message message1 = new Message("One message", "loghost-a", now());
        message1.addField(fieldName, 42);
        final Message message2 = new Message("Another message", "loghost-b", now());
        message2.addField(fieldName, "fourty-two");

        final List<Map.Entry<IndexSet, Message>> messageBatch = ImmutableList.of(
                entry(indexSet, message1),
                entry(indexSet, message2)
        );

        final List<String> failedItems = this.messages.bulkIndex(messageBatch);

        assertThat(failedItems).hasSize(1);

        final List<IndexFailure> failures = this.messages.getIndexFailureQueue().poll(1L, TimeUnit.SECONDS);

        assertThat(failures).hasSize(1);
    }

    @Test
    public void retryIndexingMessagesDuringFloodStage() throws Exception {
        triggerFloodStage(INDEX_NAME);

        final List<Map.Entry<IndexSet, Message>> messageBatch = createMessageBatch(1024 * 1024, 50);
        final Future<List<String>> result = background(() -> this.messages.bulkIndex(messageBatch));

        resetFloodStage(INDEX_NAME);

        final List<String> failedItems = result.get(3, TimeUnit.MINUTES);
        assertThat(failedItems).isEmpty();

        client().refreshNode();

        assertThat(messageCount(INDEX_NAME)).isEqualTo(50);
    }

    @Test
    public void properlySerializesCustomObjectsInMessageField() throws IOException {
        final Message message = new Message("Some message", "somesource", now());
        message.addField("custom_object", new TextNode("foo"));
        final List<Map.Entry<IndexSet, Message>> messageBatch = ImmutableList.of(
                Maps.immutableEntry(indexSet, message)
        );

        final List<String> failedItems = this.messages.bulkIndex(messageBatch);

        assertThat(failedItems).isEmpty();

        client().refreshNode();

        final ResultMessage resultMessage = this.messages.get(message.getId(), INDEX_NAME);

        assertThat(resultMessage.getMessage().getField("custom_object")).isEqualTo("foo");
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
    }

    private Map.Entry<IndexSet, Message> entry(IndexSet indexSet, Message message) {
        return new AbstractMap.SimpleEntry<>(indexSet, message);
    }

    private DateTime now() {
        return DateTime.now(DateTimeZone.UTC);
    }

    private ArrayList<Map.Entry<IndexSet, Message>> createMessageBatch(int size, int count) {
        final ArrayList<Map.Entry<IndexSet, Message>> messageList = new ArrayList<>();

        final String message = Strings.repeat('A', size);
        for (int i = 0; i < count; i++) {
            messageList.add(Maps.immutableEntry(indexSet, new Message(i + message, "source", now())));
        }
        return messageList;
    }
}
