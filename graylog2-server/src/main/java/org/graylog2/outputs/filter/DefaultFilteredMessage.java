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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.graylog2.indexer.messages.ImmutableMessage;
import org.graylog2.outputs.ElasticSearchOutput;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public record DefaultFilteredMessage(ImmutableMessage message,
                                     Multimap<String, Stream> destinations) implements FilteredMessage {
    public DefaultFilteredMessage {
        requireNonNull(message, "message cannot be null");
        requireNonNull(destinations, "destinations cannot be null");
    }

    /**
     * Creates a filtered message for the given destination keys. The streams of the given message are added to all
     * destination keys.
     *
     * @param message         the message
     * @param destinationKeys the set of destination keys
     * @return the new filtered message
     */
    public static DefaultFilteredMessage forDestinationKeys(Message message, Set<String> destinationKeys) {
        final var builder = ImmutableMultimap.<String, Stream>builder();

        destinationKeys.forEach(key -> builder.putAll(key, message.getStreams()));

        return new DefaultFilteredMessage(ImmutableMessage.wrap(message), builder.build());
    }

    @Override
    public boolean isIndexed() {
        // We consider a message indexed if it is written to the indexer output.
        return !destinations.get(ElasticSearchOutput.FILTER_KEY).isEmpty();
    }
}
