/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
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

package org.graylog2.filters;

import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;

/**
 * MessageFilter.java: 19.04.2012 11:46:59
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public interface MessageFilter {

    /**
     * Process a LogMessage
     *
     * @param msg
     * @param server
     * @return true if this message should not further be handled (for example for blacklisting purposes)
     */
    public boolean filter(LogMessage msg, GraylogServer server);
    
}
