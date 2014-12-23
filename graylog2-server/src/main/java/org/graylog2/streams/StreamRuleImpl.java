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
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Representing the rules of a single stream.
 */
@CollectionName("streamrules")
public class StreamRuleImpl extends PersistedImpl implements StreamRule {
    public static final String FIELD_TYPE = "type".intern();
    public static final String FIELD_VALUE = "value".intern();
    public static final String FIELD_FIELD = "field".intern();
    public static final String FIELD_INVERTED = "inverted".intern();
    public static final String FIELD_STREAM_ID = "stream_id".intern();
    public static final String FIELD_CONTENT_PACK = "content_pack".intern();

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

    public void setType(StreamRuleType type) {
        fields.put(FIELD_TYPE, type.toInteger());
    }

    @Override
    public String getValue() {
        return (String) fields.get(FIELD_VALUE);
    }

    public void setValue(String value) {
        fields.put(FIELD_VALUE, value);
    }

    @Override
    public String getField() {
        return (String) fields.get(FIELD_FIELD);
    }

    public void setField(String field) {
        fields.put(FIELD_FIELD, field);
    }

    public Boolean getInverted() {
        return (Boolean) firstNonNull(fields.get(FIELD_INVERTED), false);
    }

    public void setInverted(Boolean inverted) {
        fields.put(FIELD_INVERTED, inverted);
    }

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

    public Map<String, Validator> getValidations() {
        final ImmutableMap.Builder<String, Validator> validators = ImmutableMap.builder();
        validators.put(FIELD_TYPE, new IntegerValidator());
        validators.put(FIELD_FIELD, new FilledStringValidator());
        validators.put(FIELD_STREAM_ID, new ObjectIdValidator());
        validators.put(FIELD_CONTENT_PACK, new OptionalStringValidator());

        if (!this.getType().equals(StreamRuleType.PRESENCE)) {
            validators.put(FIELD_VALUE, new FilledStringValidator());
        }

        return validators.build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @JsonValue
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
