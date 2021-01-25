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
package org.graylog2.plugin.streams;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.database.Persisted;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.emptyToNull;

public interface Stream extends Persisted {
    /**
     * The ID of the default message stream for all messages.
     */
    String DEFAULT_STREAM_ID = "000000000000000000000001";
    /**
     * The ID of the default events stream for user generated events.
     */
    String DEFAULT_EVENTS_STREAM_ID = "000000000000000000000002";
    /**
     * The ID of the default events stream for system events.
     */
    String DEFAULT_SYSTEM_EVENTS_STREAM_ID = "000000000000000000000003";
    /**
     * Contains all default event streams. (e.g. events and system events)
     */
    ImmutableSet<String> DEFAULT_EVENT_STREAM_IDS = ImmutableSet.of(DEFAULT_EVENTS_STREAM_ID, DEFAULT_SYSTEM_EVENTS_STREAM_ID);

    enum MatchingType {
        AND,
        OR;

        public static final MatchingType DEFAULT = AND;

        @JsonCreator
        public static MatchingType valueOfOrDefault(String name) {
            return (emptyToNull(name) == null ? DEFAULT : valueOf(name));
        }
    }

    String getTitle();

    String getDescription();

    Boolean getDisabled();

    String getContentPack();

    void setTitle(String title);

    void setDescription(String description);

    void setDisabled(Boolean disabled);

    void setContentPack(String contentPack);

    void setMatchingType(MatchingType matchingType);

    Boolean isPaused();

    Map<String, List<String>> getAlertReceivers();

    Map<String, Object> asMap(List<StreamRule> streamRules);

    List<StreamRule> getStreamRules();

    Set<Output> getOutputs();

    MatchingType getMatchingType();

    boolean isDefaultStream();

    void setDefaultStream(boolean defaultStream);

    boolean getRemoveMatchesFromDefaultStream();

    void setRemoveMatchesFromDefaultStream(boolean removeMatchesFromDefaultStream);

    IndexSet getIndexSet();

    String getIndexSetId();

    void setIndexSetId(String indexSetId);

    static boolean isDefaultStreamId(String id) {
        return DEFAULT_STREAM_ID.equals(id) || DEFAULT_EVENT_STREAM_IDS.contains(id);
    }
}
