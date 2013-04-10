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

import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;

/**
 * Representing the rules of a single stream.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamRuleImpl implements StreamRule {

    public static final int TYPE_EXACT = 1;
    public static final int TYPE_REGEX = 2;
    public static final int TYPE_GREATER = 3;
    public static final int TYPE_SMALLER = 4;

    private ObjectId objectId = null;
    private int ruleType = 0;
    private String value = null;
    private String field = null;

    public StreamRuleImpl(DBObject rule) {
        this.objectId = (ObjectId) rule.get("_id");
        this.ruleType = (Integer) rule.get("rule_type");
        this.value = (String) rule.get("value");
        this.field = (String) rule.get("field");
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
    public int getRuleType() {
        return ruleType;
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

}
