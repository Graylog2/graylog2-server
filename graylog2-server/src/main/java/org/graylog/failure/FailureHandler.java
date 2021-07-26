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
package org.graylog.failure;

/**
 * Custom failure handlers must implement this interface and must be registered via
 * {@link com.google.inject.multibindings.Multibinder} in a corresponding plugin module.
 *
 * Example:
 *
 * Multibinder<FailureHandler> failureHandlerBinder = Multibinder.newSetBinder(binder(), FailureHandler.class);
 * failureHandlerBinder.addBinding().to(MyCustomFailureHandler.class);
 */
public interface FailureHandler {

    /**
     * An implementation of this method is expected to contain
     * the actual logic of failure handling. Should be blocking.
     */
    void handle(FailureBatch failureBatch);

    /**
     * This method guides the master failure handler service
     * when deciding whether the handler is applicable for a
     * certain batch of failures.
     */
    boolean supports(FailureBatch failureBatch);

    /**
     * Tells the master failure handling service whether the handler
     * is available for handling failures. Added to make handling configurable.
     */
    boolean isEnabled();
}
