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

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.apache.log4j.Logger;
import org.graylog2.blacklists.Blacklist;
import org.graylog2.blacklists.BlacklistRule;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class BlacklistFilter implements MessageFilter {
    
    private static final Logger LOG = Logger.getLogger(BlacklistFilter.class);

    private boolean discard;
    private final Timer processTime = Metrics.newTimer(BlacklistFilter.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    @Override
    public void filter(LogMessage msg, GraylogServer server) {
        TimerContext tcx = processTime.time();
        for (Blacklist blacklist : Blacklist.fetchAll()) {
            for (BlacklistRule rule : blacklist.getRules()) {
                if (Pattern.compile(rule.getTerm(), Pattern.DOTALL).matcher(msg.getShortMessage()).matches()) {
                    LOG.debug("Message <" + this.toString() + "> is blacklisted. First match on " + rule.getTerm());

                    // Done - This message is blacklisted.
                    discard = true;
                    return;
                }
            }
        }

        tcx.stop();
        discard = false;
    }

    @Override
    public boolean discard() {
        return discard;
    }
    
    

}
