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
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.IntegerValidator;
import org.graylog2.database.validators.ObjectIdValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Representing the rules of a single stream.
 */
@CollectionName("streamrules")
public class StreamRuleImpl extends PersistedImpl implements StreamRule {
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_VALUE = "value";
    public static final String FIELD_FIELD = "field";
    public static final String FIELD_INVERTED = "inverted";
    public static final String FIELD_STREAM_ID = "stream_id";
    public static final String FIELD_CONTENT_PACK = "content_pack";
    public static final String FIELD_DESCRIPTION = "description";

    public StreamRuleImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected StreamRuleImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public StreamRuleType getType() {
        return StreamRuleType.fromInteger((Integer) fields.get(FIELD_TYPE));
    }

    @Override
    public void setType(StreamRuleType type) {
        fields.put(FIELD_TYPE, type.toInteger());
    }

    @Override
    public String getValue() {
        return (String) fields.get(FIELD_VALUE);
    }

    @Override
    public void setValue(String value) {
        fields.put(FIELD_VALUE, value);
    }

    @Override
    public String getField() {
        return (String) fields.get(FIELD_FIELD);
    }

    @Override
    public void setField(String field) {
        fields.put(FIELD_FIELD, field);
    }

    @Override
    public Boolean getInverted() {
        return (Boolean) firstNonNull(fields.get(FIELD_INVERTED), false);
    }

    @Override
    public void setInverted(Boolean inverted) {
        fields.put(FIELD_INVERTED, inverted);
    }

    @Override
    public String getStreamId() {
        return ((ObjectId) fields.get(FIELD_STREAM_ID)).toHexString();
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
    public String getDescription() {
        return (String) fields.get(FIELD_DESCRIPTION);
    }

    @Override
    public void setDescription(String description) {
        fields.put(FIELD_DESCRIPTION, description);
    }

    @Override
    public Map<String, Validator> getValidations() {
        final ImmutableMap.Builder<String, Validator> validators = ImmutableMap.builder();
        validators.put(FIELD_TYPE, new IntegerValidator());
        validators.put(FIELD_STREAM_ID, new ObjectIdValidator());
        validators.put(FIELD_CONTENT_PACK, new OptionalStringValidator());

        if (!EnumSet.of(StreamRuleType.ALWAYS_MATCH, StreamRuleType.MATCH_INPUT).contains(this.getType())) {
            validators.put(FIELD_FIELD, new FilledStringValidator());
        }

        if (!EnumSet.of(StreamRuleType.PRESENCE, StreamRuleType.ALWAYS_MATCH).contains(this.getType())) {
            validators.put(FIELD_VALUE, new FilledStringValidator());
        }

        return validators.build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @JsonValue
    @Override
    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");
        result.put("id", getId());
        result.put(FIELD_STREAM_ID, getStreamId());

        return result;
    }

    @Override
    public String toString() {
        return ("StreamRuleImpl: <" + this.fields.toString() + ">");
    }
}
