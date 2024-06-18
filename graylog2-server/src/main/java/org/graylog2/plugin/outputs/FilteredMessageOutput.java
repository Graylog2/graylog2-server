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
package org.graylog2.plugin.outputs;

import org.graylog2.outputs.filter.FilteredMessage;

public interface FilteredMessageOutput {
    /**
     * Write the given filtered message. Based on the filtered message attributes, the output can decide
     * to discard the message.
     *
     * @param message the filtered message
     * @throws Exception if writing the message fails
     */
    void writeFiltered(FilteredMessage message) throws Exception;
}
