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
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.IntegerValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.streams.StreamRule;

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

    public static final int TYPE_EXACT = 1;
    public static final int TYPE_REGEX = 2;
    public static final int TYPE_GREATER = 3;
    public static final int TYPE_SMALLER = 4;

    private ObjectId objectId = null;
    private int type = 0;
    private String value = null;
    private String field = null;
    private Boolean inverted = false;

    /*public StreamRuleImpl(DBObject rule) {
        this.objectId = (ObjectId) rule.get("_id");
        this.ruleType = (Integer) rule.get("rule_type");
        this.value = (String) rule.get("value");
        this.field = (String) rule.get("field");
    }*/

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
    @Override
    public ObjectId getObjectId() {
        return objectId;
    }

    /**
     * @return the ruleType
     */
    @Override
    public int getType() {
        return type;
    }

    /**
     * @return the value
     */
    @Override
    public String getValue() {
        return value;
    }

	@Override
	public String getField() {
		return field;
	}

    public Boolean getInverted() {
        return inverted;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("type", new IntegerValidator());
            put("value", new FilledStringValidator());
            put("field", new FilledStringValidator());
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

        return result;
    }
}
