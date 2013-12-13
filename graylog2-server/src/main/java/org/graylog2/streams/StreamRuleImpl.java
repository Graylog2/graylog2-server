/**
 * Copyright 2011 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.streams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.*;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representing the rules of a single stream.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamRuleImpl extends Persisted implements StreamRule {

    private static final String COLLECTION = "streamrules";

    /*public static final int TYPE_EXACT = 1;
    public static final int TYPE_REGEX = 2;
    public static final int TYPE_GREATER = 3;
    public static final int TYPE_SMALLER = 4;*/

    public StreamRuleImpl(Map<String, Object> fields, Core core) {
        super(core, fields);
    }

    protected StreamRuleImpl(ObjectId id, Map<String, Object> fields, Core core) {
        super(core, id, fields);
    }

    public static StreamRuleImpl load(ObjectId id, Core core) throws NotFoundException {
        BasicDBObject o = (BasicDBObject) get(id, core, COLLECTION);

        if (o == null) {
            throw new NotFoundException();
        }

        return new StreamRuleImpl((ObjectId) o.get("_id"), o.toMap(), core);
    }

    public static List<StreamRule> findAllForStream(ObjectId streamId, Core core) throws NotFoundException {
        final List<StreamRule> streamRules = new ArrayList<StreamRule>();
        final List<DBObject> respStreamRules = StreamRuleImpl.query(
                new BasicDBObject("stream_id", streamId),
                core,
                COLLECTION
        );

        for (DBObject streamRule : respStreamRules) {
            streamRules.add(load((ObjectId)streamRule.get("_id"), core));
        }

        return streamRules;
    }

    /**
     * @return the objectId
     */
    public ObjectId getObjectId() {
        return (ObjectId) fields.get("_id");
    }

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

    public ObjectId getStreamId() {
        return (ObjectId) fields.get("stream_id");
    }

    /*public StreamImpl getStream() throws NotFoundException {
        return StreamImpl.load(getStreamId(), core);
    }*/

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("type", new IntegerValidator());
            put("value", new FilledStringValidator());
            put("field", new FilledStringValidator());
            put("stream_id", new ObjectIdValidator());
        }};
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toStringMongod());
        result.put("stream_id", getStreamId().toStringMongod());

        return result;
    }

    @Override
    public String toString() {
        return ("StreamRuleImpl: <" + this.fields.toString() + ">");
    }
}
