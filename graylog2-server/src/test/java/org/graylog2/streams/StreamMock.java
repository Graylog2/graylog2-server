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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.TestIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class StreamMock implements Stream {
    private String id;
    private String title;
    private String description;
    private boolean disabled;
    private String contentPack;
    private List<StreamRule> streamRules;
    private MatchingType matchingType;
    private boolean defaultStream;
    private boolean removeMatchesFromDefaultStream;
    private IndexSet indexSet;

    public StreamMock(Map<String, Object> stream) {
        this(stream, Collections.emptyList());
    }

    public StreamMock(Map<String, Object> stream, List<StreamRule> streamRules) {
        this.id = stream.get("_id").toString();
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
        this.indexSet = new TestIndexSet(IndexSetConfig.create(
                "index-set-id",
                "title",
                "description",
                true,
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
                IndexSetConfig.TemplateType.MESSAGES,
                1,
                false));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> getFields() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> asMap() {
        return Collections.emptyMap();
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
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public void setContentPack(String contentPack) {
        this.contentPack = contentPack;
    }

    @Override
    public Boolean isPaused() {
        return getDisabled() != null ? getDisabled() : false;
    }

    @Override
    public Map<String, List<String>> getAlertReceivers() {
        return Maps.newHashMap();
    }

    @Override
    public Map<String, Object> asMap(List<StreamRule> streamRules) {
        return Maps.newHashMap();
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
        return Sets.newHashSet();
    }


    @Override
    public MatchingType getMatchingType() {
        return this.matchingType;
    }

    @Override
    public void setMatchingType(MatchingType matchingType) {
        this.matchingType = matchingType;
    }

    @Override
    public boolean isDefaultStream() {
        return defaultStream;
    }

    @Override
    public void setDefaultStream(boolean defaultStream) {
        this.defaultStream = defaultStream;
    }

    @Override
    public boolean getRemoveMatchesFromDefaultStream() {
        return removeMatchesFromDefaultStream;
    }

    @Override
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StreamMock that = (StreamMock) o;
        return defaultStream == that.defaultStream &&
                Objects.equals(id, that.id) &&
                Objects.equals(title, that.title) &&
                Objects.equals(description, that.description) &&
                Objects.equals(streamRules, that.streamRules) &&
                Objects.equals(defaultStream, that.defaultStream) &&
                Objects.equals(removeMatchesFromDefaultStream, that.removeMatchesFromDefaultStream) &&
                matchingType == that.matchingType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, streamRules, matchingType, defaultStream, removeMatchesFromDefaultStream);
    }

    @Override
    public String getIndexSetId() {
        return "index-set-id";
    }

    @Override
    public void setIndexSetId(String indexSetId) {
    }

    @Override
    public IndexSet getIndexSet() {
        return indexSet;
    }
}
