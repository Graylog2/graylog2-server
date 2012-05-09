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

import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;

/**
 * RewriteFilter.java: 26.04.2012 16:14:47
 *
 * Describe me.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class RewriteFilter implements MessageFilter {

    private static final Logger LOG = Logger.getLogger(RewriteFilter.class);

    @Override
    public boolean filter(LogMessage msg, GraylogServer server) {
        if (server.getRulesEngine() != null) {
            server.getRulesEngine().evaluate(msg);
        }

        // Do not discard message.
        return false;
    }

}
