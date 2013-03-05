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

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.Stream;
import org.junit.Test;

public class StreamRouterTest {    
    @Test
    public void testSimple() {
        Set<StreamImpl> streams = new HashSet<StreamImpl>();
        
        FakeStream streamMatch = toStream("Matching", StreamRuleTest.toRule(StreamRuleImpl.TYPE_MESSAGE, "foo"));
        streams.add(streamMatch);
        
        streams.add(toStream("Not Matching", StreamRuleTest.toRule(StreamRuleImpl.TYPE_MESSAGE, "wazoo")));

        LogMessage msg = new LogMessage();
        msg.setShortMessage("bar foo baz");
        
        StreamRouter router = new StreamRouter();
        List<Stream> results = router.route(streams, msg);
        
        assertEquals(1, results.size());
        assertEquals(streamMatch, results.get(0));
    }

    private FakeStream toStream(String streamName, StreamRuleImpl...rules)
    {
        FakeStream stream = new FakeStream(streamName);
        for(StreamRuleImpl rule : rules) {
            stream.addRule(rule);
        }
        return stream;
    }
}
