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
package org.graylog2.outputs.filter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.outputs.FilteredMessageOutput;

import java.util.Map;
import java.util.Set;

/**
 * Adds all registered {@link FilteredMessageOutput} instances as output targets to a message.
 */
@Singleton
public class AllOutputsFilter implements OutputFilter {
    private final Set<String> outputNames;

    @Inject
    public AllOutputsFilter(Map<String, FilteredMessageOutput> outputs) {
        this.outputNames = outputs.keySet();
    }

    @Override
    public FilteredMessage apply(Message msg) {
        return DefaultFilteredMessage.forDestinationKeys(msg, outputNames);
    }
}
