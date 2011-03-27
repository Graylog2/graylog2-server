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

import org.graylog2.streams.matchers.AdditionalFieldMatcher;
import org.graylog2.streams.matchers.SeverityMatcher;
import org.graylog2.streams.matchers.HostMatcher;
import org.graylog2.streams.matchers.FacilityMatcher;
import org.graylog2.streams.matchers.MessageMatcher;
import org.graylog2.streams.matchers.StreamRuleMatcherIF;
import org.junit.Test;
import static org.junit.Assert.*;

public class StreamRuleMatcherFactoryTest {

    @Test
    public void testBuild() throws Exception {
        StreamRuleMatcherIF messageMatcher = StreamRuleMatcherFactory.build(StreamRule.TYPE_MESSAGE);
        assertTrue(messageMatcher instanceof MessageMatcher);

        StreamRuleMatcherIF hostMatcher = StreamRuleMatcherFactory.build(StreamRule.TYPE_HOST);
        assertTrue(hostMatcher instanceof HostMatcher);

        StreamRuleMatcherIF severityMatcher = StreamRuleMatcherFactory.build(StreamRule.TYPE_SEVERITY);
        assertTrue(severityMatcher instanceof SeverityMatcher);

        StreamRuleMatcherIF facilityMatcher = StreamRuleMatcherFactory.build(StreamRule.TYPE_FACILITY);
        assertTrue(facilityMatcher instanceof FacilityMatcher);

        StreamRuleMatcherIF additionalFieldMatcher = StreamRuleMatcherFactory.build(StreamRule.TYPE_ADDITIONAL);
        assertTrue(additionalFieldMatcher instanceof AdditionalFieldMatcher);
    }

    @Test
    public void testBuildWithInvalidStreamRuleType() {
        boolean exceptionThrown = false;
        try {
            StreamRuleMatcherIF messageMatcher = StreamRuleMatcherFactory.build(9001);
        } catch (InvalidStreamRuleTypeException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }

}