/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
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

    public StreamRuleMock(Map<String, Object> rule) {
        this.id = rule.get("_id").toString();
        if (rule.get(StreamRuleImpl.FIELD_TYPE) != null) {
            this.type = StreamRuleType.fromInteger((Integer) rule.get(StreamRuleImpl.FIELD_TYPE));
        }
        this.value = (String) rule.get(StreamRuleImpl.FIELD_VALUE);
        this.field = (String) rule.get(StreamRuleImpl.FIELD_FIELD);
        this.inverted = (Boolean) rule.get(StreamRuleImpl.FIELD_INVERTED);
        this.contentPack = (String) rule.get(StreamRuleImpl.FIELD_CONTENT_PACK);
    }

    public String getId() {
        return id;
    }

    public String getStreamId() {
        return streamId;
    }

    public StreamRuleType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getField() {
        return field;
    }

    public Boolean getInverted() {
        return firstNonNull(inverted, false);
    }

    public void setType(StreamRuleType type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setInverted(Boolean inverted) {
        this.inverted = inverted;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
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
