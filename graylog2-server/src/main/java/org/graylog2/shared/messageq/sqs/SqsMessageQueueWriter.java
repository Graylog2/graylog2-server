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
package org.graylog2.shared.messageq.sqs;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.io.BaseEncoding;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class SqsMessageQueueWriter extends AbstractIdleService implements MessageQueueWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SqsMessageQueueWriter.class);
    private static final int MESSAGE_SIZE_LIMIT = 256 * 1024;

    private final String queueUrl;

    private final CountDownLatch latch = new CountDownLatch(1);
    private final Meter messageMeter;
    private final Counter byteCounter;
    private final Meter byteMeter;
    private final Timer writeTimer;
    private final int shutdownTimeoutMs;

    private AmazonSQSBufferedAsyncClient sqsClient;

    @Inject
    public SqsMessageQueueWriter(MetricRegistry metricRegistry, @Named("sqs_queue_url") URI queueUrl,
            @Named("shutdown_timeout") int shutdownTimeoutMs) {
        this.queueUrl = queueUrl.toString();

        this.messageMeter = metricRegistry.meter("system.message-queue.sqs.writer.messages");
        this.byteCounter = metricRegistry.counter("system.message-queue.sqs.writer.byte-count");
        this.byteMeter = metricRegistry.meter("system.message-queue.sqs.writer.bytes");
        this.writeTimer = metricRegistry.timer("system.message-queue.sqs.writer.writes");
        this.shutdownTimeoutMs = shutdownTimeoutMs;
    }

    @Override
    protected void startUp() throws Exception {
        LOG.info("Starting sqs message queue writer service");

        final QueueBufferConfig bufferConfig = new QueueBufferConfig()
                .withFlushOnShutdown(true);
        sqsClient = new AmazonSQSBufferedAsyncClient(AmazonSQSAsyncClientBuilder.defaultClient(), bufferConfig);

        // Service is ready for writing
        latch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        if (sqsClient != null) {
            // Wait for the SQS send client to flush its buffer during shutdown but abort if that takes to long
            // TODO: test if this is really working
            // TODO: we should do this after the input buffer was drained. Check if this happens during the shutdown
            //  sequence.
            final ExecutorService executorService = Executors.newSingleThreadExecutor();
            try {
                SimpleTimeLimiter.create(executorService)
                        .runWithTimeout(sqsClient::shutdown, shutdownTimeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch  (TimeoutException e) {
                LOG.warn("Timeout waiting for SQS client to shut down. Proceeding with service shutdown.");
            }
        }
    }

    @Override
    public void write(List<RawMessageEvent> entries) throws MessageQueueException {
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.info("Got interrupted", e);
            Thread.currentThread().interrupt();
            return;
        }

        if (!isRunning()) {
            throw new MessageQueueException("Message queue service is not running");
        }

        for (final RawMessageEvent entry : entries) {
            final String body = BaseEncoding.base64()
                    .omitPadding()
                    .encode(entry.getEncodedRawMessage());
            if (body.length() > MESSAGE_SIZE_LIMIT) {
                LOG.error("Base64 encoded message <{}> of size {} bytes exceeds size limit of {} bytes. Message will " +
                                "be discarded.", entry.getMessageId(), body.length(), MESSAGE_SIZE_LIMIT);
            } else {
                sendAsync(body);
            }
        }
    }

    private void sendAsync(String body) {
        // This is not only tracking the actual time it takes to transfer the messages to SQS but also includes the
        // time a messages spends waiting in the send queue until a batch is ready to be sent out.
        final Timer.Context writeTime = writeTimer.time();
        sqsClient.sendMessageAsync(new SendMessageRequest(this.queueUrl, body),
                new AsyncHandler<SendMessageRequest, SendMessageResult>() {
            @Override
            public void onError(Exception exception) {
                LOG.error("Unable to write message to SQS.", exception);
                writeTime.stop();
            }

            @Override
            public void onSuccess(SendMessageRequest request, SendMessageResult sendMessageResult) {
                writeTime.stop();
            }
        });
        messageMeter.mark();
        byteCounter.inc(body.length());
        byteMeter.mark(body.length());
    }
}
