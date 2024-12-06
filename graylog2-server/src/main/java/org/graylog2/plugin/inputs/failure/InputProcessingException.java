package org.graylog2.plugin.inputs.failure;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.graylog2.plugin.journal.RawMessage;

import java.util.Optional;

public class InputProcessingException extends RuntimeException {


    private final InputProcessingFailure inputProcessingFailure;

    public InputProcessingException(InputProcessingFailure inputProcessingFailure) {
        super(inputProcessingFailure.errorMessage(), inputProcessingFailure.exception());
        this.inputProcessingFailure = inputProcessingFailure;
    }

    public Optional<String> inputMessageString() {
        return Optional.ofNullable(inputProcessingFailure.inputMessage());
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nullable Throwable throwable,
                                                  @Nonnull RawMessage rawMessage,
                                                  @Nullable String inputMessage) {
        return new InputProcessingException(new InputProcessingFailure(errorMessage, throwable, rawMessage, inputMessage));
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nonnull RawMessage rawMessage,
                                                  @Nullable String inputMessage) {
        return new InputProcessingException(new InputProcessingFailure(errorMessage, null, rawMessage, inputMessage));
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nullable Exception exception,
                                                  @Nonnull RawMessage rawMessage) {
        return new InputProcessingException(new InputProcessingFailure(errorMessage, exception, rawMessage, null));
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nonnull RawMessage rawMessage) {
        return new InputProcessingException(new InputProcessingFailure(errorMessage, null, rawMessage, null));
    }

}
