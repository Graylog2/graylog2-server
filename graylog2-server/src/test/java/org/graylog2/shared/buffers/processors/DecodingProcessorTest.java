package org.graylog2.shared.buffers.processors;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.graylog.failure.FailureSubmissionService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.buffers.MessageEvent;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MetricRegistry metricRegistry;

    @Mock
    private MessageQueueAcknowledger acknowledger;

    @Mock
    private FailureSubmissionService failureSubmissionService;

    @Captor
    private ArgumentCaptor<InputProcessingException> exceptionCaptor;

    private DecodingProcessor processor;

    @BeforeEach
    void setUp() {
        Timer decodeTimer = new Timer();
        Timer parseTimer = new Timer();

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

    @Test
    void validMessageIsDecodedAndSetOnEvent() throws Exception {
        setUpCodecFactory();
        when(serverStatus.getDetailedMessageRecordingStrategy())
                .thenReturn(ServerStatus.MessageDetailRecordingStrategy.NEVER);
        Message decoded = new TestMessageFactory().createMessage("message", "source", Tools.nowUTC());
        when(codec.decodeSafe(any(RawMessage.class))).thenReturn(Optional.of(decoded));

        MessageEvent event = createEvent("valid-payload");
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

        RawMessage rawMessage = createRawMessage(payload);
        InputProcessingException exception = InputProcessingException.create(
                errorMessage,
                new IllegalArgumentException("Missing field: short_message"),
                rawMessage,
                payload
        );

        when(codec.decodeSafe(any(RawMessage.class))).thenThrow(exception);

        MessageEvent event = new MessageEvent();
        event.setRaw(rawMessage);

        processor.onEvent(event, 0, true);

        verify(failureSubmissionService).submitInputFailure(exceptionCaptor.capture(), eq(INPUT_ID));

        InputProcessingException captured = exceptionCaptor.getValue();
        assertThat(captured.getMessage()).isEqualTo(errorMessage);
        assertThat(captured.getRawMessage()).isSameAs(rawMessage);
        assertThat(captured.getRawMessage().getPayload()).isEqualTo(payload.getBytes(StandardCharsets.UTF_8));

        // Message is acknowledged twice (in the catch and finally block)
        verify(acknowledger, times(2)).acknowledge(rawMessage.getMessageQueueId());
    }

    @Test
    void runtimeExceptionIsSubmittedAsFailure() throws Exception {
        setUpCodecFactory();
        RawMessage rawMessage = createRawMessage("payload");

        final String exceptionMessage = "Unable to decode raw message due to an unexpected error.";
        final String exceptionCause = "unexpected codec error.";

        when(codec.decodeSafe(any(RawMessage.class)))
                .thenThrow(new RuntimeException(exceptionCause));

        MessageEvent event = new MessageEvent();
        event.setRaw(rawMessage);

        processor.onEvent(event, 0, true);

        verify(failureSubmissionService).submitInputFailure(exceptionCaptor.capture(), eq(INPUT_ID));

        InputProcessingException captured = exceptionCaptor.getValue();
        assertThat(captured.getMessage()).isEqualTo(exceptionMessage);
        assertThat(captured.getCause()).isInstanceOf(RuntimeException.class);
        assertThat(captured.getCause().getMessage()).isEqualTo(exceptionCause);

        // Message is acknowledged twice (in the catch and finally block)
        verify(acknowledger, times(2)).acknowledge(rawMessage.getMessageQueueId());
    }

    @Test
    void missingCodecFactorySkipsMessageWithoutFailure() throws Exception {
        RawMessage rawMessage = createRawMessage("payload");
        rawMessage.setCodecName("unknown-codec");

        MessageEvent event = new MessageEvent();
        event.setRaw(rawMessage);

        processor.onEvent(event, 0, true);

        // Message is skipped when no codec factory is found
        verify(failureSubmissionService, never()).submitInputFailure(any(), any());

        verify(acknowledger).acknowledge(rawMessage.getMessageQueueId());
    }

    private MessageEvent createEvent(String payload) {
        MessageEvent event = new MessageEvent();
        event.setRaw(createRawMessage(payload));
        return event;
    }

    private RawMessage createRawMessage(String payload) {
        RawMessage raw = new RawMessage(payload.getBytes(StandardCharsets.UTF_8));
        raw.setCodecName(CODEC_NAME);
        raw.setCodecConfig(Configuration.EMPTY_CONFIGURATION);
        raw.addSourceNode(INPUT_ID, new SimpleNodeId(NODE_ID));
        return raw;
    }
}
