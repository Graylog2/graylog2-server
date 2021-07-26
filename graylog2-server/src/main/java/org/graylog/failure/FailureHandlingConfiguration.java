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
 * This interface was added to decouple failure handling configuration
 * management from the actual configuration consumption.
 */
public interface FailureHandlingConfiguration {

    /**
     * @return true if messages with failures should also be written
     * to their regular index along with a processing error message
     * in the {@link org.graylog2.plugin.Message#FIELD_GL2_PROCESSING_ERROR} field.
     *
     * See {@link org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter#handleFailedMessage}
     */
    boolean keepFailedMessageDuplicate();

    /**
     * @return true if processing failures (i.e. pipeline interpreter exceptions,
     * Extractor exceptions and etc.) should be submitted to the failure handling queue.
     */
    boolean submitProcessingFailures();
}
