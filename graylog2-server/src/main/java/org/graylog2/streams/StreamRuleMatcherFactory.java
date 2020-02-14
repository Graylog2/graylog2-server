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
package org.graylog2.streams;

import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.matchers.AlwaysMatcher;
import org.graylog2.streams.matchers.ContainsMatcher;
import org.graylog2.streams.matchers.ExactMatcher;
import org.graylog2.streams.matchers.FieldPresenceMatcher;
import org.graylog2.streams.matchers.GreaterMatcher;
import org.graylog2.streams.matchers.MatchInput;
import org.graylog2.streams.matchers.RegexMatcher;
import org.graylog2.streams.matchers.SmallerMatcher;
import org.graylog2.streams.matchers.StreamRuleMatcher;

public class StreamRuleMatcherFactory {
    public static StreamRuleMatcher build(StreamRuleType ruleType) throws InvalidStreamRuleTypeException {
        switch (ruleType) {
            case EXACT:
                return new ExactMatcher();
            case REGEX:
                return new RegexMatcher();
            case GREATER:
                return new GreaterMatcher();
            case SMALLER:
                return new SmallerMatcher();
            case PRESENCE:
                return new FieldPresenceMatcher();
            case CONTAINS:
                return new ContainsMatcher();
            case ALWAYS_MATCH:
                return new AlwaysMatcher();
            case MATCH_INPUT:
                return new MatchInput();
            default:
                throw new InvalidStreamRuleTypeException();
        }
    }
}
