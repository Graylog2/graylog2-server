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
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.IntegerValidator;
import org.graylog2.database.validators.ObjectIdValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;

import java.util.HashMap;
import java.util.Map;

/**
 * Representing the rules of a single stream.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
@CollectionName("streamrules")
public class StreamRuleImpl extends PersistedImpl implements StreamRule {

    public StreamRuleImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected StreamRuleImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    /**
     * @return the objectId
     */
/*    public ObjectId getObjectId() {
        return (ObjectId) fields.get("_id");
    }*/

    /**
     * @return the ruleType
     */
    @Override
    public StreamRuleType getType() {
        //return (Integer) fields.get("type");
        return StreamRuleType.fromInteger((Integer) fields.get("type"));
    }

    public void setType(StreamRuleType type) {
        fields.put("type", type.toInteger());
    }

    /**
     * @return the value
     */
    @Override
    public String getValue() {
        return (String) fields.get("value");
    }

    public void setValue(String value) {
        fields.put("value", value);
    }

	@Override
	public String getField() {
		return (String) fields.get("field");
	}

    public void setField(String field) {
        fields.put("field", field);
    }

    public Boolean getInverted() {
        if (fields.get("inverted") == null) {
            return false;
        }
        return (Boolean) fields.get("inverted");
    }

    public void setInverted(Boolean inverted) {
        fields.put("inverted", inverted);
    }

    public String getStreamId() {
        return ((ObjectId) fields.get("stream_id")).toStringMongod();
    }

    /*public StreamImpl getStream() throws NotFoundException {
        return StreamImpl.load(getStreamId(), core);
    }*/

    public Map<String, Validator> getValidations() {
        HashMap<String, Validator> validators = new HashMap<String, Validator>() {{
            put("type", new IntegerValidator());
            put("field", new FilledStringValidator());
            put("stream_id", new ObjectIdValidator());
        }};

        if (!this.getType().equals(StreamRuleType.PRESENCE))
            validators.put("value", new FilledStringValidator());

        return validators;
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

    @JsonValue
    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");
        result.put("id", getId());
        result.put("stream_id", getStreamId());

        return result;
    }

    @Override
    public String toString() {
        return ("StreamRuleImpl: <" + this.fields.toString() + ">");
    }
}
