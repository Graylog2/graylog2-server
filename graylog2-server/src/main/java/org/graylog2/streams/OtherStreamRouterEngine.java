/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class OtherStreamRouterEngine extends StreamRouterEngine {
    private class OtherRule extends Rule {
        private final Boolean sufficient;

        public OtherRule(Stream stream, StreamRule rule, Boolean sufficient) throws InvalidStreamRuleTypeException {
            super(stream, rule);
            this.sufficient = sufficient;
        }

        public Boolean isSufficient() {
            return sufficient;
        }
    }

    private final List<OtherRule> rulesList;

    @Inject
    public OtherStreamRouterEngine(@Assisted List<Stream> streams, @Assisted ExecutorService executorService, StreamFaultManager streamFaultManager, StreamMetrics streamMetrics) {
        super(streams, executorService, streamFaultManager, streamMetrics);
        rulesList = new LinkedList<>();

        final List<OtherRule> presenceRules = Lists.newArrayList();
        final List<OtherRule> exactRules = Lists.newArrayList();
        final List<OtherRule> greaterRules = Lists.newArrayList();
        final List<OtherRule> smallerRules = Lists.newArrayList();
        final List<OtherRule> regexRules = Lists.newArrayList();

        //final SetMultimap<String, OtherRule> fieldRules = Multimaps.newSetMultimap();

        for (Stream stream : streams) {
            final Boolean sufficient = stream.getMatchingType() == Stream.MatchingType.OR;
            for (StreamRule streamRule : stream.getStreamRules()) {
                final OtherRule otherRule;
                try {
                    otherRule = new OtherRule(stream, streamRule, sufficient);
                } catch (InvalidStreamRuleTypeException e) {
                    e.printStackTrace();
                    continue;
                }
                //fieldRules.put()
                switch (streamRule.getType()) {
                    case PRESENCE: presenceRules.add(otherRule);
                    case EXACT: exactRules.add(otherRule);
                    case GREATER: greaterRules.add(otherRule);
                    case SMALLER: smallerRules.add(otherRule);
                    case REGEX: regexRules.add(otherRule);
                }
            }
        }

        rulesList.addAll(presenceRules);
        rulesList.addAll(exactRules);
        rulesList.addAll(greaterRules);
        rulesList.addAll(smallerRules);
        rulesList.addAll(regexRules);
    }

    @Override
    public List<Stream> match(Message message) {
        final List<Stream> result = Lists.newArrayList();
        final List<Stream> blacklist = Lists.newArrayList();

        for (OtherRule otherRule : rulesList) {
            if (blacklist.contains(otherRule.getStream()))
                continue;

            final Stream stream = otherRule.match(message);

            if (stream == null) {
                if (!otherRule.isSufficient()) {
                    result.remove(otherRule.getStream());
                    // remove all rules related to this stream because stream can't match anymore
                    blacklist.add(otherRule.getStream());
                }
            } else {
                result.add(stream);
                if (otherRule.isSufficient()) {
                    // remove all rules related to this stream because stream is already matched
                    blacklist.add(stream);
                }
            }
        }

        return result;
    }
}
