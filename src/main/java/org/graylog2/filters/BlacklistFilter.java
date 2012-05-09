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

import java.util.List;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.blacklists.BlacklistRule;
import org.graylog2.logmessage.LogMessage;

/**
 * BlacklistFilter.java: 19.04.2012 13:02:39
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistFilter implements MessageFilter {

    private static final Logger LOG = Logger.getLogger(BlacklistFilter.class);

    @Override
    public boolean filter(LogMessage msg, GraylogServer server) {
        for (Blacklist blacklist : Blacklist.fetchAll()) {
            for (BlacklistRule rule : blacklist.getRules()) {
                if (Pattern.compile(rule.getTerm(), Pattern.DOTALL).matcher(msg.getShortMessage()).matches()) {
                    LOG.debug("Message <" + this.toString() + "> is blacklisted. First match on " + rule.getTerm());

                    // Done - This message is blacklisted.
                    return true;
                }
            }
        }

        return false;
    }

}
