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
package org.graylog2.plugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.List;

public class MessageCollection implements Messages  {

    private final ImmutableList<Message> messages;

    public MessageCollection(Iterable<Message> other) {
        messages = ImmutableList.copyOf(other);
    }

    @Override
    public Iterator<Message> iterator() {
        return Iterators.filter(messages.iterator(), e -> !e.getFilterOut());
    }

    @VisibleForTesting
    public List<Message> source() {
        return messages;
    }
}
