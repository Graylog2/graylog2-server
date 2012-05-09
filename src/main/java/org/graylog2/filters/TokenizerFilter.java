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

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.graylog2.GraylogServer;
import org.graylog2.logmessage.LogMessage;

/**
 * TokenizerFilter.java: 05.05.2012 12:59:24
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class TokenizerFilter implements MessageFilter {

    private static final Logger LOG = Logger.getLogger(TokenizerFilter.class);

    private Pattern p = Pattern.compile("[a-zA-Z0-9_-]*");

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
    public boolean filter(LogMessage msg, GraylogServer server) {
        int extracted = 0;
        if (msg.getShortMessage().contains("=")) {
            try {
                String[] parts = msg.getShortMessage().split(" ");
                if (parts != null) {
                    for (String part : parts) {
                        if (part.contains("=") && StringUtils.countMatches(part, "=") == 1) {
                            String[] kv = part.split("=");
                            if (kv[0] != null && kv[1] != null && p.matcher(kv[0]).matches() && !msg.getAdditionalData().containsKey("_" + kv[0])) {
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

        // We never want to filter out this message.
        return false;
    }

}
