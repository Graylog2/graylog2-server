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

import com.google.common.collect.Multimap;
import org.graylog2.indexer.messages.ImmutableMessage;
import org.graylog2.plugin.streams.Stream;

/**
 * A filtered message that contains output destination information.
 */
public interface FilteredMessage {
    /**
     * The Message object.
     *
     * @return the message
     */
    ImmutableMessage message();

    /**
     * A multimap of destination streams.
     *
     * @return multimap of destination streams
     */
    Multimap<String, Stream> destinations();

    /**
     * Whether the message has been indexed or not.
     *
     * @return true if the message has been indexed. Otherwise, false.
     */
    boolean isIndexed();
}
