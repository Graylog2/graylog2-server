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
package org.graylog.events.processor.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.indices.EventIndexer;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;

public class PersistToStreamsStorageHandler implements EventStorageHandler {
    public interface Factory extends EventStorageHandler.Factory<PersistToStreamsStorageHandler> {
        @Override
        PersistToStreamsStorageHandler create(EventStorageHandler.Config config);
    }

    private static final Logger LOG = LoggerFactory.getLogger(PersistToStreamsStorageHandler.class);

    private final Config config;
    private final EventIndexer indices;

    @Inject
    public PersistToStreamsStorageHandler(@Assisted EventStorageHandler.Config config, EventIndexer indices) {
        this.config = (Config) config;
        this.indices = indices;
    }

    @Override
    public void handleEvents(List<EventWithContext> eventsWithContext) {
        eventsWithContext.forEach(eventWithContext -> {
            config.streams().forEach(stream -> eventWithContext.event().addStream(stream));
        });
        LOG.debug("Bulk-index {} events", eventsWithContext.size());
        indices.write(eventsWithContext);
    }

    @Override
    public EventStorageHandlerCheckResult checkPreconditions() {
        return EventStorageHandlerCheckResult.canExecute(true);
    }

    @AutoValue
    @JsonTypeName(Config.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements EventStorageHandler.Config {
        public static final String TYPE_NAME = "persist-to-streams-v1";

        private static final String FIELD_STREAMS = "streams";

        @JsonProperty(FIELD_STREAMS)
        public abstract ImmutableList<String> streams();

        public static Builder builder() {
            return Builder.create();
        }

        public static Config createWithDefaultEventsStream() {
            return Builder.create()
                    .streams(ImmutableList.of(Stream.DEFAULT_EVENTS_STREAM_ID))
                    .build();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements EventStorageHandler.Config.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_PersistToStreamsStorageHandler_Config.Builder()
                        .type(TYPE_NAME)
                        .streams(ImmutableList.of(Stream.DEFAULT_EVENTS_STREAM_ID));
            }

            @JsonProperty(FIELD_STREAMS)
            public abstract Builder streams(List<String> streams);

            public abstract Config build();
        }
    }
}
