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
package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog.failure.FailureSubmissionService;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecodingProcessorTest {

    private static final String CODEC_NAME = "test-codec";
    private static final String INPUT_ID = "input-id";
    private static final String NODE_ID = "node-id";

    @Mock
    private Codec codec;

    @Mock
    private Codec.Factory<Codec> codecFactory;

    @Mock
    private ServerStatus serverStatus;

    @Mock
    private MessageQueueAcknowledger acknowledger;

    @Mock
    private FailureSubmissionService failureSubmissionService;

    @Captor
    private ArgumentCaptor<InputProcessingException> exceptionCaptor;

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final TestMessageFactory messageFactory = new TestMessageFactory();
    private DecodingProcessor processor;

    @BeforeEach
    void setUp() {
        final Timer decodeTimer = new Timer();
        final Timer parseTimer = new Timer();

        processor = new DecodingProcessor(
                Map.of(CODEC_NAME, codecFactory),
                serverStatus,
                metricRegistry,
                acknowledger,
                failureSubmissionService,
                decodeTimer,
                parseTimer
        );
    }

    private void setUpCodecFactory() {
        when(codecFactory.create(any(Configuration.class))).thenReturn(codec);
    }

    private void setUpRecordingStrategy() {
        lenient().when(serverStatus.getDetailedMessageRecordingStrategy())
                .thenReturn(ServerStatus.MessageDetailRecordingStrategy.NEVER);
    }

    // --- Single-message codec tests (from upstream PR #25025) ---

    @Test
    void validMessageIsDecodedAndSetOnEvent() throws Exception {
        setUpCodecFactory();
        setUpRecordingStrategy();
        final Message decoded = messageFactory.createMessage("message", "source", Tools.nowUTC());
        when(codec.decodeSafe(any(RawMessage.class))).thenReturn(Optional.of(decoded));

        final MessageEvent event = createEvent("valid-payload");
        assertThat(event.getMessage()).isNull();

        processor.onEvent(event, 0, true);

        verify(failureSubmissionService, never()).submitInputFailure(any(), any());
        assertThat(event.getMessage()).isNotNull();
    }

    @Test
    void inputProcessingExceptionIsSubmittedAsFailure() throws Exception {
        setUpCodecFactory();

        final String errorMessage = "GELF message is missing mandatory 'short_message' field.";
        final String payload = "invalid-gelf";

        final RawMessage rawMessage = createRawMessage(payload);
        final InputProcessingException exception = InputProcessingException.create(
                errorMessage,
                new IllegalArgumentException("Missing field: short_message"),
                rawMessage,
                payload
        );

        when(codec.decodeSafe(any(RawMessage.class))).thenThrow(exception);

        final MessageEvent event = new MessageEvent();
        event.setRaw(rawMessage);

        processor.onEvent(event, 0, true);

        verify(failureSubmissionService).submitInputFailure(exceptionCaptor.capture(), eq(INPUT_ID));

        final InputProcessingException captured = exceptionCaptor.getValue();
        assertThat(captured.getMessage()).isEqualTo(errorMessage);
        assertThat(captured.getRawMessage()).isSameAs(rawMessage);
        assertThat(captured.getRawMessage().getPayload()).isEqualTo(payload.getBytes(StandardCharsets.UTF_8));

        // Message is acknowledged twice (in the catch and finally block)
        verify(acknowledger, times(2)).acknowledge(rawMessage.getMessageQueueId());
    }

    @Test
    void runtimeExceptionIsSubmittedAsFailure() throws Exception {
        setUpCodecFactory();
        final RawMessage rawMessage = createRawMessage("payload");

        final String exceptionMessage = "Unable to decode raw message due to an unexpected error.";
        final String exceptionCause = "unexpected codec error.";

        when(codec.decodeSafe(any(RawMessage.class)))
                .thenThrow(new RuntimeException(exceptionCause));

        final MessageEvent event = new MessageEvent();
        event.setRaw(rawMessage);

        processor.onEvent(event, 0, true);

        verify(failureSubmissionService).submitInputFailure(exceptionCaptor.capture(), eq(INPUT_ID));

        final InputProcessingException captured = exceptionCaptor.getValue();
        assertThat(captured.getMessage()).isEqualTo(exceptionMessage);
        assertThat(captured.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(captured.getCause().getMessage()).isEqualTo(exceptionCause);

        // Message is acknowledged twice (in the catch and finally block)
        verify(acknowledger, times(2)).acknowledge(rawMessage.getMessageQueueId());
    }

    @Test
    void missingCodecFactorySkipsMessageWithoutFailure() throws Exception {
        final RawMessage rawMessage = createRawMessage("payload");
        rawMessage.setCodecName("unknown-codec");

        final MessageEvent event = new MessageEvent();
        event.setRaw(rawMessage);

        processor.onEvent(event, 0, true);

        // Message is skipped when no codec factory is found
        verify(failureSubmissionService, never()).submitInputFailure(any(), any());

        verify(acknowledger).acknowledge(rawMessage.getMessageQueueId());
    }

    // --- Multi-message codec tests ---

    @Test
    void multiMessageCodecDistributesInputSizeProportionally() throws Exception {
        setUpRecordingStrategy();
        final MultiMessageCodec multiCodec = setUpMultiMessageCodec();

        // 1000-byte payload decoded into 3 messages with different content sizes
        final byte[] payload = new byte[1000];
        final RawMessage raw = createRawMessage(payload);

        final Message msg1 = messageFactory.createMessage("short", "source", Tools.nowUTC());
        final Message msg2 = messageFactory.createMessage("a medium length message", "source", Tools.nowUTC());
        final Message msg3 = messageFactory.createMessage("this is a significantly longer message with more content for testing", "source", Tools.nowUTC());

        when(multiCodec.decodeMessages(any(RawMessage.class))).thenReturn(List.of(msg1, msg2, msg3));

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        final Collection<Message> result = event.getMessages();
        assertThat(result).hasSize(3);

        final List<Message> resultList = List.copyOf(result);
        for (final Message msg : resultList) {
            assertThat((Long) msg.getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE))
                    .as("Each message should have a positive input size")
                    .isGreaterThan(0L);
        }

        // Total assigned input size must equal the original payload length
        final long totalInputSize = resultList.stream()
                .mapToLong(m -> (Long) m.getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE))
                .sum();
        assertThat(totalInputSize).isEqualTo(payload.length);

        // Larger messages should get a larger share of the input size
        final long size1 = (Long) resultList.get(0).getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE);
        final long size2 = (Long) resultList.get(1).getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE);
        final long size3 = (Long) resultList.get(2).getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE);
        assertThat(size1).isLessThan(size2);
        assertThat(size2).isLessThan(size3);
    }

    @Test
    void singleMessageCodecAssignsFullPayloadSize() throws Exception {
        setUpCodecFactory();
        setUpRecordingStrategy();

        final byte[] payload = new byte[750];
        final RawMessage raw = createRawMessage(payload);

        final Message msg = messageFactory.createMessage("test", "source", Tools.nowUTC());
        when(codec.decodeSafe(any(RawMessage.class))).thenReturn(Optional.of(msg));

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        assertThat(event.getMessage()).isNotNull();
        assertThat((Long) event.getMessage().getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE))
                .isEqualTo(750L);
    }

    @Test
    void multiMessageCodecWithSingleMessageAssignsFullPayloadSize() throws Exception {
        setUpRecordingStrategy();
        final MultiMessageCodec multiCodec = setUpMultiMessageCodec();

        final byte[] payload = new byte[300];
        final RawMessage raw = createRawMessage(payload);

        final Message msg = messageFactory.createMessage("only message", "source", Tools.nowUTC());
        when(multiCodec.decodeMessages(any(RawMessage.class))).thenReturn(List.of(msg));

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        final List<Message> resultList = List.copyOf(event.getMessages());
        assertThat(resultList).hasSize(1);
        assertThat((Long) resultList.get(0).getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE))
                .isEqualTo(300L);
    }

    @Test
    void inputMessageSizeFromRawMessageTakesPrecedence() throws Exception {
        setUpCodecFactory();
        setUpRecordingStrategy();

        final byte[] payload = new byte[900];
        final RawMessage raw = createRawMessage(payload);
        raw.setInputMessageSize(300);

        final Message msg = messageFactory.createMessage("test", "source", Tools.nowUTC());
        when(codec.decodeSafe(any(RawMessage.class))).thenReturn(Optional.of(msg));

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        assertThat(event.getMessage()).isNotNull();
        assertThat((Long) event.getMessage().getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE))
                .isEqualTo(300L);
    }

    // --- Traffic accounting tests ---

    @Test
    void singleMessageCodecIncrementsDecodedTrafficCounter() throws Exception {
        setUpCodecFactory();
        setUpRecordingStrategy();

        final byte[] payload = "hello world test message".getBytes(StandardCharsets.UTF_8);
        final RawMessage raw = createRawMessage(payload);

        final Message msg = messageFactory.createMessage("decoded content", "source", Tools.nowUTC());
        when(codec.decodeSafe(any(RawMessage.class))).thenReturn(Optional.of(msg));

        final long decodedTrafficBefore = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC).getCount();

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        final long decodedTrafficAfter = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC).getCount();

        assertThat(event.getMessage()).isNotNull();
        assertThat(decodedTrafficAfter - decodedTrafficBefore).isEqualTo(event.getMessage().getSize());
    }

    @Test
    void singleMessageCodecAccountedSizeReflectsDecodedContent() throws Exception {
        setUpCodecFactory();
        setUpRecordingStrategy();

        final byte[] payload = new byte[500];
        final RawMessage raw = createRawMessage(payload);

        final Message msg = messageFactory.createMessage("test content for accounting", "test-source", Tools.nowUTC());
        when(codec.decodeSafe(any(RawMessage.class))).thenReturn(Optional.of(msg));

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        assertThat(event.getMessage()).isNotNull();
        final long accountedSize = event.getMessage().getSize();
        assertThat(accountedSize)
                .as("Accounted size should be positive and reflect decoded field content, not raw payload size")
                .isGreaterThan(0L)
                .isNotEqualTo(500L);
    }

    @Test
    void multiMessageCodecIncrementsDecodedTrafficBySumOfMessageSizes() throws Exception {
        setUpRecordingStrategy();
        final MultiMessageCodec multiCodec = setUpMultiMessageCodec();

        final byte[] payload = new byte[600];
        final RawMessage raw = createRawMessage(payload);

        final Message msg1 = messageFactory.createMessage("short", "source", Tools.nowUTC());
        final Message msg2 = messageFactory.createMessage("a longer message with more content", "source", Tools.nowUTC());

        when(multiCodec.decodeMessages(any(RawMessage.class))).thenReturn(List.of(msg1, msg2));

        final long decodedTrafficBefore = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC).getCount();

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        final long decodedTrafficAfter = metricRegistry.counter(GlobalMetricNames.DECODED_TRAFFIC).getCount();
        final long expectedTrafficIncrease = event.getMessages().stream()
                .mapToLong(Message::getSize)
                .sum();

        assertThat(decodedTrafficAfter - decodedTrafficBefore).isEqualTo(expectedTrafficIncrease);
    }

    @Test
    void multiMessageCodecWithInputMessageSizeUsesEffectiveSize() throws Exception {
        setUpRecordingStrategy();
        final MultiMessageCodec multiCodec = setUpMultiMessageCodec();

        final byte[] payload = new byte[1000];
        final RawMessage raw = createRawMessage(payload);
        raw.setInputMessageSize(500);

        final Message msg1 = messageFactory.createMessage("same message", "source", Tools.nowUTC());
        final Message msg2 = messageFactory.createMessage("same message", "source", Tools.nowUTC());

        when(multiCodec.decodeMessages(any(RawMessage.class))).thenReturn(List.of(msg1, msg2));

        final MessageEvent event = new MessageEvent();
        event.setRaw(raw);

        processor.onEvent(event, 0, true);

        final List<Message> resultList = List.copyOf(event.getMessages());
        assertThat(resultList).hasSize(2);

        final long size1 = (Long) resultList.get(0).getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE);
        final long size2 = (Long) resultList.get(1).getField(Message.FIELD_GL2_INPUT_MESSAGE_SIZE);

        assertThat(size1 + size2)
                .as("Distribution should use inputMessageSize (500), not payload length (1000)")
                .isEqualTo(500L);
        assertThat(size1).isEqualTo(size2);
    }

    // --- Helpers ---

    /**
     * Sets up the codec factory to return a MultiMessageCodec mock.
     * Returns the mock so tests can configure decodeMessages().
     */
    private MultiMessageCodec setUpMultiMessageCodec() {
        final MultiMessageCodec multiCodec = mock(MultiMessageCodec.class);
        lenient().when(multiCodec.getConfiguration()).thenReturn(Configuration.EMPTY_CONFIGURATION);
        when(codecFactory.create(any(Configuration.class))).thenReturn(multiCodec);
        return multiCodec;
    }

    private MessageEvent createEvent(String payload) {
        final MessageEvent event = new MessageEvent();
        event.setRaw(createRawMessage(payload));
        return event;
    }

    private RawMessage createRawMessage(String payload) {
        return createRawMessage(payload.getBytes(StandardCharsets.UTF_8));
    }

    private RawMessage createRawMessage(byte[] payload) {
        final RawMessage raw = new RawMessage(payload);
        raw.setCodecName(CODEC_NAME);
        raw.setCodecConfig(Configuration.EMPTY_CONFIGURATION);
        raw.addSourceNode(INPUT_ID, new SimpleNodeId(NODE_ID));
        return raw;
    }
}
