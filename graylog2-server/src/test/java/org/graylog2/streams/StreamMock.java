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
package org.graylog2.streams;

import com.google.common.base.MoreObjects;
import org.bson.types.ObjectId;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.indexer.MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE;

public class StreamMock implements Stream {
    private String id;
    private String title;
    private String description;
    private boolean disabled;
    private String contentPack;
    private List<StreamRule> streamRules;
    private List<String> categories;
    private MatchingType matchingType;
    private boolean defaultStream;
    private boolean removeMatchesFromDefaultStream;
    private IndexSet indexSet;
    private Set<Output> outputs;

    public StreamMock(Map<String, Object> stream) {
        this(stream, Collections.emptyList());
    }

    public StreamMock(Map<String, Object> stream, List<StreamRule> streamRules) {
        this.id = stream.getOrDefault("_id", ObjectId.get()).toString();
        this.title = (String) stream.get(StreamImpl.FIELD_TITLE);
        this.description = (String) stream.get(StreamImpl.FIELD_DESCRIPTION);
        if (stream.containsKey(StreamImpl.FIELD_DISABLED)) {
            this.disabled = (boolean) stream.get(StreamImpl.FIELD_DISABLED);
        }
        this.contentPack = (String) stream.get(StreamImpl.FIELD_CONTENT_PACK);
        this.streamRules = streamRules;
        this.matchingType = (MatchingType) stream.getOrDefault(StreamImpl.FIELD_MATCHING_TYPE, MatchingType.AND);
        this.defaultStream = (boolean) stream.getOrDefault(StreamImpl.FIELD_DEFAULT_STREAM, false);
        this.removeMatchesFromDefaultStream = (boolean) stream.getOrDefault(StreamImpl.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, false);
        //noinspection unchecked
        this.categories = (List<String>) stream.getOrDefault(StreamImpl.FIELD_CATEGORIES, List.of());
        this.outputs = Set.of();
        this.indexSet = new TestIndexSet(IndexSetConfig.create(
                "index-set-id",
                "title",
                "description",
                true, true,
                "prefix",
                1,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.createDefault(),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.createDefault(),
                ZonedDateTime.of(2017, 3, 29, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "template",
                MESSAGE_TEMPLATE_TYPE,
                1,
                false));
    }

    public StreamMock(ObjectId id, Map<String, Object> fields, List<StreamRule> streamRules, Set<Output> outputs, @Nullable IndexSet indexSet) {
        this(fields, streamRules);
        this.id = id.toString();
        this.outputs = outputs;
        this.indexSet = indexSet;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCreatorUserId() {
        return "mock-user";
    }

    @Override
    public DateTime getCreatedAt() {
        return null;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Boolean getDisabled() {
        return disabled;
    }

    @Override
    public String getContentPack() {
        return contentPack;
    }

    @Override
    public List<String> getCategories() {
        return categories;
    }

    @Override
    public Boolean isPaused() {
        return getDisabled() != null ? getDisabled() : false;
    }

    @Override
    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public void setStreamRules(List<StreamRule> streamRules) {
        this.streamRules = streamRules;
    }

    @Override
    public Set<Output> getOutputs() {
        return outputs;
    }

    @Override
    public Set<ObjectId> getOutputIds() {
        return outputs.stream().map(o -> new ObjectId(o.getId())).collect(Collectors.toSet());
    }

    @Override
    public MatchingType getMatchingType() {
        return this.matchingType;
    }

    @Override
    public boolean isDefaultStream() {
        return defaultStream;
    }

    @Override
    public boolean getRemoveMatchesFromDefaultStream() {
        return removeMatchesFromDefaultStream;
    }

    public void setRemoveMatchesFromDefaultStream(boolean removeMatchesFromDefaultStream) {
        this.removeMatchesFromDefaultStream = removeMatchesFromDefaultStream;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(StreamMock.class)
                .add("id", id)
                .add("title", title)
                .add("matchingType", matchingType)
                .add("defaultStream", defaultStream)
                .add("disabled", disabled)
                .add("removeMatchesFromDefaultStream", removeMatchesFromDefaultStream)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StreamMock that = (StreamMock) o;
        return defaultStream == that.defaultStream &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(streamRules, that.streamRules) &&
                Objects.equals(removeMatchesFromDefaultStream, that.removeMatchesFromDefaultStream) &&
                matchingType == that.matchingType;
    }

    @Override
    public int hashCode() {
        return getFingerprint();
    }

    @Override
    public int getFingerprint() {
        return Objects.hash(id, removeMatchesFromDefaultStream, matchingType.toString());
    }

    @Override
    public String getIndexSetId() {
        return "index-set-id";
    }

    @Override
    public IndexSet getIndexSet() {
        return indexSet;
    }
}
