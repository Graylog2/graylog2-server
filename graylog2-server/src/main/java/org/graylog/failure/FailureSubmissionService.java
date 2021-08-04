package org.graylog.failure;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Singleton
public class FailureSubmissionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FailureSubmissionQueue failureSubmissionQueue;
    private final FailureHandlingConfiguration failureHandlingConfiguration;

    @Inject
    public FailureSubmissionService(
            FailureSubmissionQueue failureSubmissionQueue,
            FailureHandlingConfiguration failureHandlingConfiguration) {
        this.failureSubmissionQueue = failureSubmissionQueue;
        this.failureHandlingConfiguration = failureHandlingConfiguration;
    }


    public boolean submitUnknownProcessingError(Message message, String details) {
        return submitProcessingErrorsInternal(message, ImmutableList.of(new Message.ProcessingError(
                ProcessingFailureCause.UNKNOWN,
                "Encountered an unrecognizable processing error",
                details)));
    }

    public boolean submitProcessingErrors(Message message) {
        return submitProcessingErrorsInternal(message, message.processingErrors());
    }

    private boolean submitProcessingErrorsInternal(Message message, List<Message.ProcessingError> processingErrors) {
        if (!failureHandlingConfiguration.submitProcessingFailures()) {
            // We don't handle processing errors
            return true;
        }
        if (processingErrors.isEmpty()) {
            return true;
        }
        if (!failureHandlingConfiguration.keepFailedMessageDuplicate()) {
            message.setFilterOut(true);
        }

        processingErrors.forEach(pe -> submitProcessingFailure(message, pe));

        return !message.getFilterOut();
    }

    private void submitProcessingFailure(Message failedMessage, Message.ProcessingError processingError) {
        try {
            // If we store the regular message, the acknowledgement happens in the output path
            final boolean needsAcknowledgement = !failureHandlingConfiguration.keepFailedMessageDuplicate();

            final String message;

            if (StringUtils.isBlank(failedMessage.getMessageId())) {
                message = String.format(Locale.ENGLISH,
                        "Failed to process a message with unknown id. %s",
                        processingError.getMessage());
            } else {
                message = String.format(Locale.ENGLISH,
                        "Failed to process message with id '%s'. %s",
                        failedMessage.getMessageId(),
                        processingError.getMessage());
            }

            final ProcessingFailure processingFailure = new ProcessingFailure(
                    processingError.getCause(),
                    message,
                    processingError.getDetails(),
                    Tools.nowUTC(),
                    failedMessage,
                    needsAcknowledgement);

            failureSubmissionQueue.submitBlocking(FailureBatch.processingFailureBatch(processingFailure));
        } catch (InterruptedException ignored) {
            logger.warn("Failed to submit a processing failure for failure handling. The thread has been interrupted!");
            Thread.currentThread().interrupt();
        }
    }

    public void submitIndexingErrors(Collection<Messages.IndexingError> indexingErrors) {
        try {
            failureSubmissionQueue.submitBlocking(FailureBatch.indexingFailureBatch(
                    indexingErrors.stream()
                            .map(this::fromIndexingError)
                            .collect(Collectors.toList())));

        } catch (InterruptedException ignored) {
            logger.warn("Failed to submit {} indexing errors for failure handling. The thread has been interrupted!",
                    indexingErrors.size());
            Thread.currentThread().interrupt();
        }
    }

    private IndexingFailure fromIndexingError(Messages.IndexingError indexingError) {
        return new IndexingFailure(
                indexingError.errorType() == Messages.IndexingError.ErrorType.MappingError ?
                        IndexingFailureCause.MappingError : IndexingFailureCause.UNKNOWN,
                String.format(Locale.ENGLISH,
                        "Failed to index message with id '%s' targeting '%s'",
                        indexingError.message().getMessageId(), indexingError.index()),
                indexingError.errorMessage(),
                Tools.nowUTC(),
                indexingError.message(),
                indexingError.index()
        );
    }
}
