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

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.indexer.IndexSet;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representing a single stream from the streams collection. Also provides method
 * to get all streams of this collection.
 */
@CollectionName("streams")
public class StreamImpl extends PersistedImpl implements Stream {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_RULES = "rules";
    public static final String FIELD_OUTPUTS = "outputs";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_ALERT_RECEIVERS = "alert_receivers";
    public static final String FIELD_DISABLED = "disabled";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    public static final String FIELD_MATCHING_TYPE = "matching_type";
    public static final String FIELD_DEFAULT_STREAM = "is_default_stream";
    public static final String FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM = "remove_matches_from_default_stream";
    public static final String FIELD_INDEX_SET_ID = "index_set_id";
    public static final String EMBEDDED_ALERT_CONDITIONS = "alert_conditions";

    private final List<StreamRule> streamRules;
    private final Set<Output> outputs;
    private final IndexSet indexSet;

    public StreamImpl(Map<String, Object> fields) {
        super(fields);
        this.streamRules = null;
        this.outputs = null;
        this.indexSet = null;
    }

    public StreamImpl(Map<String, Object> fields, IndexSet indexSet) {
        super(fields);
        this.streamRules = null;
        this.outputs = null;
        this.indexSet = indexSet;
    }

    protected StreamImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
        this.streamRules = null;
        this.outputs = null;
        this.indexSet = null;
    }

    public StreamImpl(ObjectId id, Map<String, Object> fields, List<StreamRule> streamRules, Set<Output> outputs, @Nullable IndexSet indexSet) {
        super(id, fields);

        this.streamRules = streamRules;
        this.outputs = outputs;
        this.indexSet = indexSet;
    }

    @Override
    public String toString() {
        return this.id.toString() + ": \"" + this.getTitle() + "\"";
    }

    @Override
    public List<StreamRule> getStreamRules() {
        return this.streamRules;
    }

    @Override
    public Set<Output> getOutputs() {
        return this.outputs;
    }

    @Override
    public String getTitle() {
        return (String) fields.get(FIELD_TITLE);
    }

    @Override
    public String getDescription() {
        return (String) fields.get(FIELD_DESCRIPTION);
    }

    @Override
    public void setTitle(String title) {
        fields.put(FIELD_TITLE, title);
    }

    @Override
    public void setDescription(String description) {
        fields.put(FIELD_DESCRIPTION, description);
    }

    @Override
    public Boolean getDisabled() {
        return (Boolean) fields.getOrDefault(FIELD_DISABLED, false);
    }

    @Override
    public void setDisabled(Boolean disabled) {
        fields.put(FIELD_DISABLED, disabled);
    }

    @Override
    public String getContentPack() {
        return (String) fields.get(FIELD_CONTENT_PACK);
    }

    @Override
    public void setContentPack(String contentPack) {
        fields.put(FIELD_CONTENT_PACK, contentPack);
    }

    @Override
    public Boolean isPaused() {
        Boolean disabled = getDisabled();
        return disabled != null && disabled;
    }

    @Override
    public Map<String, Object> asMap(List<StreamRule> streamRules) {
        Map<String, Object> result = asMap();

        List<Map<String, Object>> streamRulesMap = Lists.newArrayList();

        for (StreamRule streamRule : streamRules) {
            streamRulesMap.add(streamRule.asMap());
        }

        result.put(FIELD_RULES, streamRulesMap);

        return result;
    }

    @JsonValue
    @Override
    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toHexString());
        result.remove(FIELD_CREATED_AT);
        result.put(FIELD_CREATED_AT, Tools.getISO8601String((DateTime) fields.get(FIELD_CREATED_AT)));
        result.put(FIELD_RULES, streamRules);
        result.put(FIELD_OUTPUTS, outputs);
        result.put(FIELD_MATCHING_TYPE, getMatchingType());
        result.put(FIELD_DEFAULT_STREAM, isDefaultStream());
        result.put(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, getRemoveMatchesFromDefaultStream());
        result.put(FIELD_INDEX_SET_ID, getIndexSetId());
        return result;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(FIELD_TITLE, new FilledStringValidator())
                .put(FIELD_CREATOR_USER_ID, new FilledStringValidator())
                .put(FIELD_CREATED_AT, new DateValidator())
                .put(FIELD_CONTENT_PACK, new OptionalStringValidator())
                .put(FIELD_INDEX_SET_ID, new FilledStringValidator())
                .build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        if (EMBEDDED_ALERT_CONDITIONS.equals(key)) {
            return ImmutableMap.of(
                    "id", new FilledStringValidator(),
                    "parameters", new MapValidator());
        }

        return Collections.emptyMap();
    }

    @Override
    public Map<String, List<String>> getAlertReceivers() {
        @SuppressWarnings("unchecked")
        final Map<String, List<String>> alertReceivers =
                (Map<String, List<String>>) fields.getOrDefault(FIELD_ALERT_RECEIVERS, Collections.emptyMap());
        return alertReceivers;
    }

    @Override
    public MatchingType getMatchingType() {
        final String matchingTypeString = (String) fields.get(FIELD_MATCHING_TYPE);

        if (matchingTypeString == null) {
            return MatchingType.AND;
        } else {
            return MatchingType.valueOf(matchingTypeString);
        }
    }

    @Override
    public void setMatchingType(MatchingType matchingType) {
        fields.put(FIELD_MATCHING_TYPE, matchingType.toString());
    }

    @Override
    public boolean isDefaultStream() {
        return (boolean) fields.getOrDefault(FIELD_DEFAULT_STREAM, false);
    }

    @Override
    public void setDefaultStream(boolean defaultStream) {
        fields.put(FIELD_DEFAULT_STREAM, defaultStream);
    }

    @Override
    public boolean getRemoveMatchesFromDefaultStream() {
        return (boolean) fields.getOrDefault(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, false);
    }

    @Override
    public void setRemoveMatchesFromDefaultStream(boolean removeMatchesFromDefaultStream) {
        fields.put(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, removeMatchesFromDefaultStream);
    }

    @Override
    public IndexSet getIndexSet() {
        // The indexSet might be null because of backwards compatibility but it shouldn't be for regular streams.
        // Throw an exception if indexSet is not set to avoid losing messages!
        if (indexSet == null) {
            throw new IllegalStateException("index set must not be null! (stream id=" + getId() + " title=\"" + getTitle() + "\")");
        }
        return indexSet;
    }

    @Override
    public String getIndexSetId() {
        return (String) fields.get(FIELD_INDEX_SET_ID);
    }

    @Override
    public void setIndexSetId(String indexSetId) {
        fields.put(FIELD_INDEX_SET_ID, indexSetId);
    }
}
