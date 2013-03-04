/**
 * Copyright 2011, 2012, 2013 Lennart Koopmann <lennart@socketfeed.com>
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

import java.util.regex.Pattern;
import org.graylog2.plugin.logmessage.LogMessage;
import org.graylog2.plugin.streams.StreamRule;

/**
 * @author Dennis Oelkers <dennis@lauschmusik.de>
 */
public class FullMessageMatcher implements StreamRuleMatcher {

    private final Pattern pattern;
    public FullMessageMatcher(StreamRule rule) {
        pattern = Pattern.compile(rule.getValue(), Pattern.DOTALL);
    }

    @Override
    public boolean match(LogMessage msg, StreamRule rule) {
        String fullMessage = msg.getFullMessage();
        return fullMessage != null && pattern.matcher(fullMessage).find();
    }
}