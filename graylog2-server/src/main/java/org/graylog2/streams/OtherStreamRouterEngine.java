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
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;

import javax.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

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
                    case PRESENCE:
                        presenceRules.add(otherRule);
                        break;
                    case EXACT:
                        exactRules.add(otherRule);
                        break;
                    case GREATER:
                        greaterRules.add(otherRule);
                        break;
                    case SMALLER:
                        smallerRules.add(otherRule);
                        break;
                    case REGEX:
                        regexRules.add(otherRule);
                        break;
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
        final Set<Stream> result = Sets.newHashSet();
        final Set<Stream> blackList = Sets.newHashSet();

        for (final OtherRule otherRule : rulesList) {
            if (blackList.contains(otherRule.getStream())) {
                continue;
            }

            final StreamRule streamRule = otherRule.getStreamRule();
            if (streamRule.getType() != StreamRuleType.PRESENCE && !message.hasField(streamRule.getField())) {
                continue;
            }

            final Stream stream;
            if (streamRule.getType() != StreamRuleType.REGEX) {
                stream = otherRule.match(message);
            } else {
                stream = matchWithTimeOut(message, otherRule);
            }

            if (stream == null) {
                if (!otherRule.isSufficient()) {
                    result.remove(otherRule.getStream());
                    // blacklist stream because it can't match anymore
                    blackList.add(otherRule.getStream());
                }
            } else {
                result.add(stream);
                if (otherRule.isSufficient()) {
                    // blacklist stream because it is already matched
                    blackList.add(otherRule.getStream());
                }
            }
        }

        return Lists.newArrayList(result);
    }

    private Stream matchWithTimeOut(final Message message, final OtherRule otherRule) {
        Stream matchedStream = null;
        try {
            matchedStream = timeLimiter.callWithTimeout(new Callable<Stream>() {
                @Override
                public Stream call() throws Exception {
                    return otherRule.match(message);
                }
            }, streamProcessingTimeout, TimeUnit.MILLISECONDS, true);
        } catch (Exception e) {
            streamFaultManager.registerFailure(otherRule.getStream());
        }

        return matchedStream;
    }
}
