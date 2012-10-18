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
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class TokenizerFilter implements MessageFilter {

    private static final Logger LOG = Logger.getLogger(TokenizerFilter.class);

    private final Pattern p = Pattern.compile("[a-zA-Z0-9_-]*");
    private final Pattern kvPattern = Pattern.compile("\\s?=\\s?");
    private final Pattern spacePattern = Pattern.compile(" ");
    private final Timer processTime = Metrics.newTimer(TokenizerFilter.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

    /*
     * Extract out only true k=v pairs, not everything separated by a = character.
     *
     * This means:
     *   Extract [k1=v1, k2=v2]: Ohai I am a message k1=v1 k2=v2 Awesome!
     *   Extract nothing: Ohai I am a message and this is a URL: index.php?foo=bar&baz=bar
     *
     * Also, do not overwrite stuff that may have already been extracted as structured syslog.
     *
     * See the tests for more examples.
     * 
     */

    @Override
    public void filter(LogMessage msg, GraylogServer server) {
        TimerContext tcx = processTime.time();

        int extracted = 0;
        if (msg.getShortMessage().contains("=")) {
            try {
                final String nmsg = kvPattern.matcher(msg.getShortMessage()).replaceAll("=");
                final String[] parts = spacePattern.split(nmsg);
                if (parts != null) {
                    for (String part : parts) {
                        if (part.contains("=") && StringUtils.countMatches(part, "=") == 1) {
                            String[] kv = part.split("=");
                            if (kv.length == 2 && p.matcher(kv[0]).matches() && !msg.getAdditionalData().containsKey("_" + kv[0]) && !kv[0].equals("id")) {
                                msg.addAdditionalData(kv[0].trim(), kv[1].trim());
                                extracted++;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                LOG.warn("Error while trying to tokenize message <" + msg.getId() + ">", e);
            }
        }

        LOG.debug("Extracted <" + extracted + "> additional fields from message <" + msg.getId() + "> k=v pairs.");

        tcx.stop();
    }

    @Override
    public boolean discard() {
        return false;
    }

}
