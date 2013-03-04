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

import org.bson.types.ObjectId;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.matchers.AdditionalFieldMatcher;
import org.graylog2.streams.matchers.FacilityRegexMatcher;
import org.graylog2.streams.matchers.FileNameAndLineMatcher;
import org.graylog2.streams.matchers.FullMessageMatcher;
import org.graylog2.streams.matchers.HostRegexMatcher;
import org.graylog2.streams.matchers.SeverityMatcher;
import org.graylog2.streams.matchers.HostMatcher;
import org.graylog2.streams.matchers.FacilityMatcher;
import org.graylog2.streams.matchers.MessageMatcher;
import org.graylog2.streams.matchers.SeverityOrHigherMatcher;
import org.graylog2.streams.matchers.StreamRuleMatcher;
import org.junit.Test;

import com.mongodb.BasicDBObject;

import static org.junit.Assert.*;

public class StreamRuleMatcherFactoryTest {

    @Test
    public void testBuild() throws InvalidStreamRuleTypeException {
    	checkMatcherClassForType(StreamRuleImpl.TYPE_MESSAGE, "bar", MessageMatcher.class);
    	checkMatcherClassForType(StreamRuleImpl.TYPE_FULL_MESSAGE, "bar", FullMessageMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_HOST, "bar", HostMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_SEVERITY, "3", SeverityMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_FACILITY, "bar", FacilityMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_ADDITIONAL, "field=value", AdditionalFieldMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_SEVERITY_OR_HIGHER, "3", SeverityOrHigherMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_HOST_REGEX, "bar", HostRegexMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_FILENAME_LINE, "bar:3", FileNameAndLineMatcher.class);
		checkMatcherClassForType(StreamRuleImpl.TYPE_FACILITY_REGEX, "bar", FacilityRegexMatcher.class);
    }

    @Test(expected=InvalidStreamRuleTypeException.class)
    public void testBuildWithInvalidStreamRuleType() throws InvalidStreamRuleTypeException {
        StreamRuleMatcherFactory.build(toRule(9001, "bar4"));
    }
    
    private static void checkMatcherClassForType(int type, String value, Class<? extends StreamRuleMatcher> clazz) throws InvalidStreamRuleTypeException {
        StreamRuleMatcher matcher = StreamRuleMatcherFactory.build(toRule(type, value));
        assertTrue("Expected " + clazz.getName() + ", but got " + matcher.getClass().getName(), clazz.isInstance(matcher));
    }
    
    private static StreamRule toRule(int type, String value) {
        BasicDBObject mongo = new BasicDBObject();
        mongo.put("_id", new ObjectId());
        mongo.put("rule_type", type);
        mongo.put("value", value);
        
    	return new StreamRuleImpl(mongo);
    }

}