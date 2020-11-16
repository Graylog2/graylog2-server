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
package org.graylog2.plugin.filters;

import org.graylog2.plugin.Message;

public interface MessageFilter {
    /**
     * Process a Message
     *
     * @return true if this message should not further be handled (for example for blacklisting purposes)
     */
    boolean filter(Message msg);

    /**
     * @return The name of this filter. Should not include whitespaces or special characters.
     */
    String getName();

    /**
     * For determining the runtime order of the filter, specify a priority.
     * Lower priority values are run earlier, if two filters have the same priority, their name will be compared to
     * guarantee a repeatable order.
     *
     * @return the priority
     */
    int getPriority();
}
