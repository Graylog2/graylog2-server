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
