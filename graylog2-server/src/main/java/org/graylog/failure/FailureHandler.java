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
 * A handler for failures, occurring at different stages of message processing
 * (e.g. pipeline processing, extraction, Elasticsearch indexing). To register
 * a handler implementation you need to inform Guice about the new dependency
 * via {@link com.google.inject.multibindings.Multibinder}:
 *
 * <pre>{@code
 * Multibinder<FailureHandler> failureHandlerBinder = Multibinder.newSetBinder(binder(), FailureHandler.class);
 * failureHandlerBinder.addBinding().to(MyCustomFailureHandler.class);
 * }</pre>
 */
public interface FailureHandler {

    /**
     * Handles a batch of failures
     *
     * @param failureBatch a batch of failures, supported by this handler
     */
    void handle(FailureBatch failureBatch);

    /**
     * Guides the main failure handling service, when deciding
     * whether this handler is suitable for a certain batch of failures
     *
     * @param failureBatch a batch of failures to test
     * @return true if the batch can be processed by this handler
     */
    boolean supports(FailureBatch failureBatch);

    /**
     * Guides the main failure handling service, when deciding
     * whether this handler is available for processing.
     *
     * @return true if this handler can accept failure batches
     */
    boolean isEnabled();
}
