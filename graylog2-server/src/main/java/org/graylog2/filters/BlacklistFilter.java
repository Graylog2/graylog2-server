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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.blacklists.BlacklistRule;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.Message;

import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistFilter implements MessageFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(BlacklistFilter.class);

    @Override
    public boolean filter(Message msg, GraylogServer server) {
        for (Blacklist blacklist : Blacklist.fetchAll()) {
            for (BlacklistRule rule : blacklist.getRules()) {
                if (Pattern.compile(rule.getTerm(), Pattern.DOTALL).matcher(msg.getMessage()).matches()) {
                    LOG.debug("Message <{}> is blacklisted. First match on {}", this, rule.getTerm());

                    // Done - This message is blacklisted.
                    return true;
                }
            }
        }

        return false;
    }
    
    @Override
    public String getName() {
        return "Blacklister";
    }

}
