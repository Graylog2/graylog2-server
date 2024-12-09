package org.graylog2.plugin.inputs.failure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog2.plugin.journal.RawMessage;

public record InputProcessingFailure(@Nonnull String errorMessage,
                                     @Nullable Throwable exception,
                                     @Nonnull RawMessage rawMessage,
                                     @Nullable String inputMessage) {
}
