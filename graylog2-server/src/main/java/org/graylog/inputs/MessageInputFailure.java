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
package org.graylog.inputs;

import jakarta.annotation.Nullable;
import org.slf4j.event.Level;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

public record MessageInputFailure(Class<?> loggingClass, Level level, String message, @Nullable Throwable exception) {
    public MessageInputFailure {
        requireNonNull(loggingClass, "loggingClass cannot be null");
        requireNonNull(level, "level cannot be null");
        requireNonBlank(message, "message cannot be blank");
    }

    public static MessageInputFailure asWarning(Class<?> loggingClass, String message, Throwable exception) {
        return new MessageInputFailure(loggingClass, Level.WARN, message, exception);
    }

    public static MessageInputFailure asError(Class<?> loggingClass, String message, Throwable exception) {
        return new MessageInputFailure(loggingClass, Level.ERROR, message, exception);
    }

    public String detailedMessage() {
        if (exception != null) {
            return message + ": (" + exception.getMessage() + ")";
        } else {
            return message;
        }
    }
}
