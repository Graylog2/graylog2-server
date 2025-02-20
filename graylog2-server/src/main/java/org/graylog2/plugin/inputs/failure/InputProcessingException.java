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

    private final RawMessage rawMessage;
    private final String inputMessage;

    public InputProcessingException(String errorMessage,
                                    Throwable throwable,
                                    RawMessage rawMessage,
                                    String inputMessage) {
        super(errorMessage, throwable);
        this.rawMessage = rawMessage;
        this.inputMessage = inputMessage;
    }

    public Optional<String> inputMessage() {
        return Optional.ofNullable(inputMessage);
    }

    public RawMessage getRawMessage() {
        return rawMessage;
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nullable Throwable throwable,
                                                  @Nonnull RawMessage rawMessage,
                                                  @Nullable String inputMessage) {
        return new InputProcessingException(errorMessage, throwable, rawMessage, inputMessage);
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nonnull RawMessage rawMessage,
                                                  @Nullable String inputMessage) {
        return new InputProcessingException(errorMessage, null, rawMessage, inputMessage);
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nullable Exception exception,
                                                  @Nonnull RawMessage rawMessage) {
        return new InputProcessingException(errorMessage, exception, rawMessage, null);
    }

    public static InputProcessingException create(@Nonnull String errorMessage,
                                                  @Nonnull RawMessage rawMessage) {
        return new InputProcessingException(errorMessage, null, rawMessage, null);
    }

}
