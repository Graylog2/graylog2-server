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

package org.graylog2.streams.matchers;

import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleTest;
import org.junit.Test;
import static org.junit.Assert.*;

public class FileNameAndLineMatcherTest {

    @Test
    public void testSuccessfulMath() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_FILENAME_LINE, "^main\\.rb:1\\d");

        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher(rule);
        
        LogMessage msg = new LogMessage();
        msg.setFile("main.rb");
        msg.setLine(17);
        
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithoutRegex() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_FILENAME_LINE, "lol\\.php:9001");

        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher(rule);
        
        LogMessage msg = new LogMessage();
        msg.setFile("lol.php");
        msg.setLine(9001);

        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testSuccessfulMatchWithoutLineNumber() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_FILENAME_LINE, "^lol\\.php");

        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher(rule);

        LogMessage msg = new LogMessage();
        msg.setFile("lol.php");
        
        assertTrue(matcher.match(msg, rule));
    }

    @Test
    public void testMissedMatch() {
        StreamRule rule = StreamRuleTest.toRule(StreamRuleImpl.TYPE_FILENAME_LINE, "^main\\.rb:1\\d");

        FileNameAndLineMatcher matcher = new FileNameAndLineMatcher(rule);

        LogMessage msg = new LogMessage();
        msg.setFile("main.rb");
        msg.setLine(27);
        
        assertFalse(matcher.match(msg, rule));
    }
}