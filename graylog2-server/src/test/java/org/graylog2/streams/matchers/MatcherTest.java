/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams.matchers;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.InvalidStreamRuleTypeException;
import org.graylog2.streams.StreamRuleMatcherFactory;
import org.graylog2.streams.StreamTestHelpers;

public class MatcherTest {
    protected StreamRule.Builder getSampleRuleBuilder() {
        return StreamTestHelpers.streamRuleBuilder()
            .field("something");
    }

    protected Message getSampleMessage() {
        return new Message("foo", "bar", Tools.nowUTC());
    }

    protected StreamRuleMatcher getMatcher(StreamRule rule) {
        StreamRuleMatcher matcher;
        try {
            matcher = StreamRuleMatcherFactory.build(rule.getType());
        } catch (InvalidStreamRuleTypeException e) {
            return null;
        }

        return matcher;
    }
}
