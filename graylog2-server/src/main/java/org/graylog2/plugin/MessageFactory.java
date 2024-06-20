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

import jakarta.annotation.Nullable;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.IndexingResultCallback;
import org.joda.time.DateTime;

import java.util.Map;

public interface MessageFactory {
    /**
     * Returns a new {@link Message} object for the given fields.
     *
     * @param message   the message field value
     * @param source    the source field value
     * @param timestamp the timestamp field value
     * @return the new message object
     */
    Message createMessage(String message, String source, DateTime timestamp);

    /**
     * Returns a new {@link Message} object for the given fields map.
     *
     * @param fields the map of fields
     * @return the new message object
     */
    Message createMessage(Map<String, Object> fields);

    /**
     * Returns a new {@link Message} object for the given ID and fields.
     *
     * @param id        the message ID
     * @param newFields the map of fields
     * @return the new message object
     */
    Message createMessage(String id, Map<String, Object> newFields);

    /**
     * Return a new {@link SystemMessage} object which can be used for System purposes like restoring Archives.
     * The message has the following properties:
     * <ul>
     *  <li>A size of 0, so its traffic is not accounted</li>
     *  <li>A single predetermined IndexSet</li>
     *  <li>No streams, so it will only be routed to the {@link org.graylog2.outputs.DefaultMessageOutput}</li>
     * </ul>
     *
     * @param indexSet       the predetermined indexSet where the message will be indexed
     * @param fields         the map of fields
     * @param resultCallback an optional {@link IndexingResultCallback} that will be called once the Message is indexed
     * @return the new SystemMessage object
     */
    public SystemMessage createSystemMessage(IndexSet indexSet, Map<String, Object> fields, @Nullable IndexingResultCallback resultCallback);

    /**
     * Returns a fake {@link Message}. This message must not be used for real message processing!
     *
     * @return a fake message
     */
    static Message createFakeMessage() {
        return new Message("__fake", "__fake", DateTime.parse("2010-07-30T16:03:25Z")); // first Graylog release
    }
}
