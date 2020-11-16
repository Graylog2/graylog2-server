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
package org.graylog2.streams.matchers;

import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.StreamRuleImpl;

import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

public class StreamRuleMock implements StreamRule {
    private String id;
    private String streamId;
    private StreamRuleType type = null;
    private String value;
    private String field;
    private Boolean inverted;
    private String contentPack;
    private String description;

    public StreamRuleMock(Map<String, Object> rule) {
        this.id = rule.get("_id").toString();
        if (rule.get(StreamRuleImpl.FIELD_TYPE) != null) {
            this.type = StreamRuleType.fromInteger((Integer) rule.get(StreamRuleImpl.FIELD_TYPE));
        }
        this.value = (String) rule.get(StreamRuleImpl.FIELD_VALUE);
        this.field = (String) rule.get(StreamRuleImpl.FIELD_FIELD);
        this.inverted = (Boolean) rule.get(StreamRuleImpl.FIELD_INVERTED);
        this.contentPack = (String) rule.get(StreamRuleImpl.FIELD_CONTENT_PACK);
        this.streamId = (String) rule.get(StreamRuleImpl.FIELD_STREAM_ID);
        this.description = (String) rule.get(StreamRuleImpl.FIELD_DESCRIPTION);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getStreamId() {
        return streamId;
    }

    @Override
    public StreamRuleType getType() {
        return type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public Boolean getInverted() {
        return firstNonNull(inverted, false);
    }

    @Override
    public void setType(StreamRuleType type) {
        this.type = type;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void setField(String field) {
        this.field = field;
    }

    @Override
    public void setInverted(Boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getContentPack() {
        return contentPack;
    }

    @Override
    public void setContentPack(String contentPack) {
        this.contentPack = contentPack;
    }

    @Override
    public Map<String, Object> getFields() {
        return null;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return null;
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }

    @Override
    public Map<String, Object> asMap() {
        return null;
    }
}
