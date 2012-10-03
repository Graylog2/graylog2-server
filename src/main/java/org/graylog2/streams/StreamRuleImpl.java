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

    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_HOST = 2;
    public static final int TYPE_SEVERITY = 3;
    public static final int TYPE_FACILITY = 4;
    // Type 5 is reserved for frontend usage (timeframe filter)
    public static final int TYPE_ADDITIONAL = 6;
    // Type 7 used to be for the removed hostgroup feature.
    public static final int TYPE_SEVERITY_OR_HIGHER = 8;
    public static final int TYPE_HOST_REGEX = 9;
    public static final int TYPE_FULL_MESSAGE = 10;
    public static final int TYPE_FILENAME_LINE = 11;

    private ObjectId objectId = null;
    private int ruleType = 0;
    private String value = null;

    public StreamRuleImpl(DBObject rule) {
        this.objectId = (ObjectId) rule.get("_id");
        this.ruleType = (Integer) rule.get("rule_type");
        this.value = (String) rule.get("value");
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



}
