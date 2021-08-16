package org.graylog2.shared.messageq.localkafka;

import com.google.common.collect.ImmutableList;
import org.graylog2.plugin.Message;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class LocalKafkaMessageQueueAcknowledgerTest {

    @Mock
    LocalKafkaJournal kafkaJournal;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MessageQueueAcknowledger.Metrics metrics;

    @InjectMocks
    LocalKafkaMessageQueueAcknowledger acknowledger;

    @Test
    void acknowledgeOffset() {
        acknowledger.acknowledge(1L);
        verify(kafkaJournal).markJournalOffsetCommitted(1L);
    }

    @Test
    void acknowledgeNullOffset() {
        acknowledger.acknowledge((Long) null);
        verifyNoMoreInteractions(kafkaJournal);
    }

    @Test
    void acknowledgeMessage() {
        final Message message = new Message("message", "source", DateTime.now());
        message.setMessageQueueId(1L);
        acknowledger.acknowledge(message);
        verify(kafkaJournal).markJournalOffsetCommitted(1L);
    }

    @Test
    void acknowledgeMessageWithoutMessageQueueId() {
        final Message message = new Message("message", "source", DateTime.now());
        acknowledger.acknowledge(message);
        verifyNoMoreInteractions(kafkaJournal);
    }

    @Test
    void acknowledgeMessageWithWrongTypeOfMessageQueueId() {
        final Message message = new Message("message", "source", DateTime.now());
        message.setMessageQueueId("foo");
        acknowledger.acknowledge(message);
        verifyNoMoreInteractions(kafkaJournal);
    }

    @Test
    void acknowledgeMessages() {
        final Message firstMessage = new Message("message", "source", DateTime.now());
        firstMessage.setMessageQueueId(1L);

        final Message nullOffsetMessage = new Message("message", "source", DateTime.now());

        final Message secondMessage = new Message("message", "source", DateTime.now());
        secondMessage.setMessageQueueId(2L);

        final Message wrongOffsetTypeMessage = new Message("message", "source", DateTime.now());
        wrongOffsetTypeMessage.setMessageQueueId("foo");

        acknowledger.acknowledge(ImmutableList.of(firstMessage, nullOffsetMessage, secondMessage, wrongOffsetTypeMessage));

        verify(kafkaJournal).markJournalOffsetCommitted(2L);
    }
}
