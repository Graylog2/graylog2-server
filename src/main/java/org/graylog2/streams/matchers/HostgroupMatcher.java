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

import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.StreamRule;

/**
 * HostgroupMatcher.java: Apr 15, 2011 12:00:20 AM
 *
 * [description]
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class HostgroupMatcher implements StreamRuleMatcherIF {

    public boolean match(GELFMessage msg, StreamRule rule) {
        throw new UnsupportedOperationException();
        // Get hostgroup. Interate over it and return true if a host in there is matched.
        //return msg.getHost().equals(rule.getValue());
    }

}