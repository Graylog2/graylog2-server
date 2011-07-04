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

package org.graylog2.streams;

import org.apache.log4j.Logger;
import org.graylog2.messagehandlers.gelf.GELFMessage;
import org.graylog2.streams.matchers.StreamRuleMatcherIF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Router.java: Mar 16, 2011 9:40:24 PM
 *
 * Routes a GELF Message to it's streams.
 *
 * @author: Lennart Koopmann <lennart@socketfeed.com>
 */
public class Router {

    private static final Logger LOG = Logger.getLogger(Router.class);

    // Hidden.
    private Router() { }

    public static List<Stream> route(GELFMessage msg) {
        ArrayList<Stream> matches = new ArrayList<Stream>();
        ArrayList<Stream> streams = null;
        try {
            streams = Stream.fetchAllEnabled();
        } catch (Exception e) {
            LOG.error("Could not fetch streams: " + e.getMessage(), e);
        }

        for (Stream stream : streams) {

            if (stream.getStreamRules().isEmpty()) {
                continue;
            }

            HashMap<Integer, ArrayList<StreamRule>> rules = new HashMap<Integer, ArrayList<StreamRule>>();
            HashMap<Integer, Boolean> streamMatch = new HashMap<Integer, Boolean>();

            // Build a hash of rules organized by rule type.
            for (StreamRule rule : stream.getStreamRules()) {
                Integer ruleType = rule.getRuleType();

                if (!rules.containsKey(ruleType)) {
                    rules.put(ruleType, new ArrayList<StreamRule>());
                }

                if (!streamMatch.containsKey(ruleType)) {
                    streamMatch.put(ruleType, false);
                }

                rules.get(ruleType).add(rule);
            }

            // Check to see if ANY rule of a given rule type matches.
            for (Integer ruleType : rules.keySet()) {
                boolean match = false;

                for (StreamRule rule : rules.get(ruleType)) {

                    try {
                        StreamRuleMatcherIF matcher = StreamRuleMatcherFactory.build(ruleType);

                        if (msg.matchStreamRule(matcher, rule)) {
                            match = true;
                            break;
                        }
                    } catch (InvalidStreamRuleTypeException e) {
                        LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
                    }
                }

                if (match) {
                    streamMatch.put(ruleType, true);
                }
            }

            if (!streamMatch.containsValue(false)) {
                matches.add(stream);
            }
        }

        return matches;
    }

}
