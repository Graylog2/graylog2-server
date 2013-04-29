/**
 * Copyright 2011, 2012 Lennart Koopmann <lennart@socketfeed.com>
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.streams.matchers.StreamRuleMatcher;

import java.util.List;

import com.google.common.collect.Lists;
import org.graylog2.Core;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

/**
 * Routes a GELF Message to it's streams.
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class StreamRouter {

    private static final Logger LOG = LoggerFactory.getLogger(StreamRouter.class);

    public List<Stream> route(Core server, Message msg) {
        List<Stream> matches = Lists.newArrayList();
        List<Stream> streams = StreamImpl.loadAllEnabled(server);

        for (Stream stream : streams) {
            boolean missed = false;

            if (stream.getStreamRules().isEmpty()) {
                continue;
            }

            for (StreamRule rule : stream.getStreamRules()) {
                try {
                    StreamRuleMatcher matcher = StreamRuleMatcherFactory.build(rule.getRuleType());
                    if (!matchStreamRule(msg, matcher, rule)) {
                        missed = true;
                        break;
                    }
                } catch (InvalidStreamRuleTypeException e) {
                    LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
                }
            }

            // All rules were matched.
            if (!missed) {
                matches.add(stream);
            }
        }

        return matches;
    }

    public boolean matchStreamRule(Message msg, StreamRuleMatcher matcher, StreamRule rule) {
        try {
            return matcher.match(msg, rule);
        } catch (Exception e) {
            LOG.warn("Could not match stream rule <" + rule.getRuleType() + "/" + rule.getValue() + ">: " + e.getMessage(), e);
            return false;
        }
    }

}