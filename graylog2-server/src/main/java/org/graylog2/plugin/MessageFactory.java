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
     * Returns a new {@link Message} that is excluded from license/traffic accounting:
     * {@link Message#isAccounted()} is {@code false}, so it counts against neither output-based nor
     * input-based license traffic, and its accounted size ({@link Message#getSize()}, stored as
     * {@code gl2_accounted_message_size}) is {@code 0}. The message is otherwise indexed and processed
     * normally.
     * <p>
     * Use this only for messages that are genuinely outside the user's control and must not consume
     * licensed quota (e.g. Graylog Collector self-logs). The exclusion is fixed at construction and
     * cannot be changed afterward.
     *
     * @param message   the message field value
     * @param source    the source field value
     * @param timestamp the timestamp field value
     * @return the new, unaccounted message object
     */
    Message createUnaccountedMessage(String message, String source, DateTime timestamp);

    /**
     * Returns a new {@link Message} object for the given fields map.
     *
     * @param fields the map of fields
     * @return the new message object
     */
    Message createMessage(Map<String, Object> fields);

    /**
     * Like {@link #createMessage(Map)}, but the returned message is excluded from license/traffic
     * accounting ({@link Message#isAccounted()} is {@code false}). See
     * {@link #createUnaccountedMessage(String, String, DateTime)} for when this is appropriate.
     *
     * @param fields the map of fields
     * @return the new, unaccounted message object
     */
    Message createUnaccountedMessage(Map<String, Object> fields);

    /**
     * Returns a new {@link Message} object for the given ID and fields.
     *
     * @param id        the message ID
     * @param newFields the map of fields
     * @return the new message object
     */
    Message createMessage(String id, Map<String, Object> newFields);

    /**
     * Like {@link #createMessage(String, Map)}, but the returned message is excluded from license/traffic
     * accounting ({@link Message#isAccounted()} is {@code false}). See
     * {@link #createUnaccountedMessage(String, String, DateTime)} for when this is appropriate.
     *
     * @param id        the message ID
     * @param newFields the map of fields
     * @return the new, unaccounted message object
     */
    Message createUnaccountedMessage(String id, Map<String, Object> newFields);

    /**
     * Returns a fake {@link Message}. This message must not be used for real message processing!
     *
     * @return a fake message
     */
    static Message createFakeMessage() {
        return new Message("__fake", "__fake", DateTime.parse("2010-07-30T16:03:25Z")); // first Graylog release
    }
}
