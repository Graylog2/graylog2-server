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

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.matchers.StreamRuleMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Stream routing engine to select matching streams for a message.
 *
 * This class is NOT thread-safe! Use one instance per thread.
 */
public class StreamRouterEngine {
    private static final Logger LOG = LoggerFactory.getLogger(StreamRouterEngine.class);

    private final List<Stream> streams;
    private final StreamFaultManager streamFaultManager;
    private final StreamMetrics streamMetrics;
    private final TimeLimiter timeLimiter;
    private final long streamProcessingTimeout;
    private final String fingerprint;

    private final List<Rule> rulesList;

    public interface Factory {
        StreamRouterEngine create(List<Stream> streams, ExecutorService executorService);
    }

    @Inject
    public StreamRouterEngine(@Assisted List<Stream> streams,
                              @Assisted ExecutorService executorService,
                              StreamFaultManager streamFaultManager,
                              StreamMetrics streamMetrics) {
        this.streams = streams;
        this.streamFaultManager = streamFaultManager;
        this.streamMetrics = streamMetrics;
        this.timeLimiter = new SimpleTimeLimiter(executorService);
        this.streamProcessingTimeout = streamFaultManager.getStreamProcessingTimeout();
        this.fingerprint = new StreamListFingerprint(streams).getFingerprint();

        final List<Rule> presenceRules = Lists.newArrayList();
        final List<Rule> exactRules = Lists.newArrayList();
        final List<Rule> greaterRules = Lists.newArrayList();
        final List<Rule> smallerRules = Lists.newArrayList();
        final List<Rule> regexRules = Lists.newArrayList();

        for (Stream stream : streams) {
            final boolean sufficient = stream.getMatchingType() == Stream.MatchingType.OR;
            for (StreamRule streamRule : stream.getStreamRules()) {
                final Rule rule;
                try {
                    rule = new Rule(stream, streamRule, sufficient);
                } catch (InvalidStreamRuleTypeException e) {
                    LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
                    continue;
                }
                switch (streamRule.getType()) {
                    case PRESENCE:
                        presenceRules.add(rule);
                        break;
                    case EXACT:
                        exactRules.add(rule);
                        break;
                    case GREATER:
                        greaterRules.add(rule);
                        break;
                    case SMALLER:
                        smallerRules.add(rule);
                        break;
                    case REGEX:
                        regexRules.add(rule);
                        break;
                }
            }
        }

        this.rulesList = new ImmutableList.Builder<Rule>()
                .addAll(presenceRules)
                .addAll(exactRules)
                .addAll(greaterRules)
                .addAll(smallerRules)
                .addAll(regexRules)
                .build();
    }

    /**
     * Returns the list of streams that are processed by the engine.
     *
     * @return the list of streams
     */
    public List<Stream> getStreams() {
        return streams;
    }

    /**
     * Returns the fingerprint of the engine instance.
     *
     * @return the fingerprint
     */
    public String getFingerprint() {
        return fingerprint;
    }

    /**
     * Returns a list of matching streams for the given message.
     *
     * @param message the message
     * @return the list of matching streams
     */
    public List<Stream> match(Message message) {
        final Set<Stream> result = Sets.newHashSet();
        final Set<Stream> blackList = Sets.newHashSet();

        for (final Rule rule : rulesList) {
            if (blackList.contains(rule.getStream())) {
                continue;
            }

            final StreamRule streamRule = rule.getStreamRule();
            if (streamRule.getType() != StreamRuleType.PRESENCE && !message.hasField(streamRule.getField())) {
                continue;
            }

            final Stream stream;
            if (streamRule.getType() != StreamRuleType.REGEX) {
                stream = rule.match(message);
            } else {
                stream = matchWithTimeOut(message, rule);
            }

            if (stream == null) {
                if (!rule.isSufficient()) {
                    result.remove(rule.getStream());
                    // blacklist stream because it can't match anymore
                    blackList.add(rule.getStream());
                }
            } else {
                result.add(stream);
                if (rule.isSufficient()) {
                    // blacklist stream because it is already matched
                    blackList.add(rule.getStream());
                }
            }
        }


        for (Stream stream : result) {
            streamMetrics.markIncomingMeter(stream.getId());
        }

        return Lists.newArrayList(result);
    }

    private Stream matchWithTimeOut(final Message message, final Rule rule) {
        Stream matchedStream = null;
        try {
            matchedStream = timeLimiter.callWithTimeout(new Callable<Stream>() {
                @Override
                public Stream call() throws Exception {
                    return rule.match(message);
                }
            }, streamProcessingTimeout, TimeUnit.MILLISECONDS, true);
        } catch (Exception e) {
            streamFaultManager.registerFailure(rule.getStream());
        }

        return matchedStream;
    }

    /**
     * Returns a list of stream rule matches. Can be used to test streams and stream rule matches.
     * This is meant for testing, do NOT use in production processing pipeline! (use {@link #match(org.graylog2.plugin.Message) match} instead)
     *
     * @param message the message to match streams on
     */
    public List<StreamTestMatch> testMatch(Message message) {
        final List<StreamTestMatch> matches = Lists.newArrayList();

        for (final Stream stream : streams) {
            final StreamTestMatch match = new StreamTestMatch(stream);

            for (final StreamRule streamRule : stream.getStreamRules()) {
                try {
                    final Rule rule = new Rule(stream, streamRule, null);
                    match.addRule(rule);
                } catch (InvalidStreamRuleTypeException e) {
                    LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
                }
            }

            match.matchMessage(message);

            matches.add(match);
        }

        return matches;
    }

    private class Rule {
        private final Stream stream;
        private final StreamRule rule;
        private final StreamRuleMatcher matcher;
        private final Boolean sufficient;


        public Rule(Stream stream, StreamRule rule, Boolean sufficient) throws InvalidStreamRuleTypeException {
            this.stream = stream;
            this.rule = rule;
            this.sufficient = sufficient;
            this.matcher = StreamRuleMatcherFactory.build(rule.getType());
        }

        public Boolean isSufficient() {
            return sufficient;
        }

        public Stream match(Message message) {
            // TODO Add missing message recordings!
            try (final Timer.Context timer = streamMetrics.getExecutionTimer(rule.getId()).time()) {
                if (matcher.match(message, rule)) {
                    return stream;
                } else {
                    return null;
                }
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error matching stream rule <" + rule.getType() + "/" + rule.getValue() + ">: " + e.getMessage(), e);
                }
                streamMetrics.markExceptionMeter(rule.getStreamId());
                return null;
            }
        }

        public StreamRule getStreamRule() {
            return rule;
        }

        public Stream getStream() {
            return stream;
        }
    }

    /**
     * Contains matching results for a stream. This is useful for testing to see if a stream matches and which
     * rules matched.
     */
    public static class StreamTestMatch {
        private final Stream stream;
        private final List<Rule> rules = Lists.newArrayList();
        private final Stream.MatchingType matchingType;

        private final Map<StreamRule, Boolean> matches = Maps.newHashMap();

        public StreamTestMatch(Stream stream) {
            this.stream = stream;
            this.matchingType = stream.getMatchingType();
        }

        public void addRule(Rule rule) {
            rules.add(rule);
        }

        public void matchMessage(Message message) {
            for (Rule rule : rules) {
                final Stream match = rule.match(message);
                matches.put(rule.getStreamRule(), (match != null && match.equals(stream)));
            }
        }

        public boolean isMatched() {
            switch (matchingType) {
                case OR:
                    return matches.values().contains(false);
                case AND:
                default:
                    return !matches.values().contains(false);
            }
        }

        public Stream getStream() {
            return stream;
        }

        public Map<StreamRule, Boolean> getMatches() {
            return matches;
        }
    }
}
