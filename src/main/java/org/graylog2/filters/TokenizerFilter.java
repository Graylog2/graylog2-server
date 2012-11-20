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

import com.google.common.base.CharMatcher;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.plugin.GraylogServer;
import org.graylog2.plugin.filters.MessageFilter;
import org.graylog2.plugin.logmessage.LogMessage;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class TokenizerFilter implements MessageFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TokenizerFilter.class);
    private static final Pattern p = Pattern.compile("[a-zA-Z0-9_-]*");
    private static final Pattern kvPattern = Pattern.compile("\\s?=\\s?");
    private static final Pattern spacePattern = Pattern.compile(" ");
    private static final Pattern quotedValuePattern = Pattern.compile("([a-zA-Z0-9_-]+=\"[^\"]+\")");
    private static final CharMatcher QUOTE_MATCHER = CharMatcher.is('"').precomputed();
    private static final CharMatcher EQUAL_SIGN_MATCHER = CharMatcher.is('=').precomputed();
    private static final Timer processTime = Metrics.newTimer(TokenizerFilter.class, "ProcessTime", TimeUnit.MICROSECONDS, TimeUnit.SECONDS);

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
        TimerContext tcx = processTime.time();

        int extracted = 0;
        if (msg.getShortMessage().contains("=")) {
            try {
                final String nmsg = kvPattern.matcher(msg.getShortMessage()).replaceAll("=");
                if (nmsg.contains("=\"")) {
                    Matcher m = quotedValuePattern.matcher(nmsg);
                    while (m.find()) {
                        String[] kv = m.group(1).split("=");
                        if (kv.length == 2 && p.matcher(kv[0]).matches() && !kv[0].equals("id")) {
                            msg.addAdditionalData(kv[0].trim(), QUOTE_MATCHER.removeFrom(kv[1]).trim());
                            extracted++;
                        }
                    }
                } else {
                    final String[] parts = spacePattern.split(nmsg);
                    if (parts != null) {
                        for (String part : parts) {
                            if (part.contains("=") && EQUAL_SIGN_MATCHER.countIn(part) == 1) {
                                String[] kv = part.split("=");
                                if (kv.length == 2 && p.matcher(kv[0]).matches() && !msg.getAdditionalData().containsKey("_" + kv[0]) && !kv[0].equals("id")) {
                                    msg.addAdditionalData(kv[0].trim(), kv[1].trim());
                                    extracted++;
                                }
                            }
                        }
                    }
                }

            } catch(Exception e) {
                LOG.warn("Error while trying to tokenize message <" + msg.getId() + ">", e);
            }
        }

        LOG.debug("Extracted <{}> additional fields from message <{}> k=v pairs.", extracted, msg.getId());

        tcx.stop();
        return false;
    }
    
    @Override
    public String getName() {
        return "Tokenizer";
    }}
