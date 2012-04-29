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

import org.graylog2.logmessage.LogMessage;
import org.graylog2.streams.StreamRule;

/**
 * FacilityMatcher.java: Mar 27, 2011 5:33:46 PM
 *
 * [description]
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class FacilityMatcher implements StreamRuleMatcher {

    @Override
    public boolean match(LogMessage msg, StreamRule rule) {
        return msg.getFacility().equals(rule.getValue());
    }

}