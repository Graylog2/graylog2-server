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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import javax.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.matchers.StreamRuleMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Map<String, List<Rule>> presenceRules = Maps.newHashMap();
    private final Map<String, List<Rule>> exactRules = Maps.newHashMap();
    private final Map<String, List<Rule>> greaterRules = Maps.newHashMap();
    private final Map<String, List<Rule>> smallerRules = Maps.newHashMap();
    private final Map<String, List<Rule>> regexRules = Maps.newHashMap();

    private final Set<String> presenceFields = Sets.newHashSet();
    private final Set<String> exactFields = Sets.newHashSet();
    private final Set<String> greaterFields = Sets.newHashSet();
    private final Set<String> smallerFields = Sets.newHashSet();
    private final Set<String> regexFields = Sets.newHashSet();

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

        for (final Stream stream : streams) {
            for (final StreamRule streamRule : stream.getStreamRules()) {
                try {
                    final Rule rule = new Rule(stream, streamRule);

                    switch (streamRule.getType()) {
                        case EXACT:
                            addRule(exactRules, exactFields, streamRule.getField(), rule);
                            break;
                        case GREATER:
                            addRule(greaterRules, greaterFields, streamRule.getField(), rule);
                            break;
                        case SMALLER:
                            addRule(smallerRules, smallerFields, streamRule.getField(), rule);
                            break;
                        case REGEX:
                            addRule(regexRules, regexFields, streamRule.getField(), rule);
                            break;
                        case PRESENCE:
                            addRule(presenceRules, presenceFields, streamRule.getField(), rule);
                            break;
                    }
                } catch (InvalidStreamRuleTypeException e) {
                    LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
                }
            }
        }
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
        final Map<Stream, StreamMatch> matches = Maps.newHashMap();
        final Set<Stream> timeouts = Sets.newHashSet();
        final List<Stream> result = Lists.newArrayList();
        final Set<String> fieldNames = message.getFieldNames();

        // Execute the rules ordered by complexity. (fast rules first)
        matchRules(message, presenceFields, presenceRules, matches);
        // Only pass an intersection of the rules fields to avoid checking every field! (does not work for presence matching)
        matchRules(message, Sets.intersection(fieldNames, exactFields), exactRules, matches);
        matchRules(message, Sets.intersection(fieldNames, greaterFields), greaterRules, matches);
        matchRules(message, Sets.intersection(fieldNames, smallerFields), smallerRules, matches);
        // Execute regex rules with a timeout to prevent bad regexes to hang the processing.
        matchRulesWithTimeout(message, Sets.intersection(fieldNames, regexFields), regexRules, matches, timeouts);

        // Register failure for streams where rules ran into a timeout.
        for (Stream stream : timeouts) {
            streamFaultManager.registerFailure(stream);
        }

        for (Map.Entry<Stream, StreamMatch> entry : matches.entrySet()) {
            if (entry.getValue().isMatched()) {
                result.add(entry.getKey());
                streamMetrics.markIncomingMeter(entry.getKey().getId());
            }
        }

        return result;
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
                    final Rule rule = new Rule(stream, streamRule);
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

    private void matchRules(Message message, Set<String> fields, Map<String, List<Rule>> rules, Map<Stream, StreamMatch> matches) {
        for (String field : fields) {
            for (Rule rule : rules.get(field)) {
                registerMatch(matches, rule.match(message));
            }
        }
    }

    private void matchRulesWithTimeout(final Message message, Set<String> fields, Map<String, List<Rule>> rules, Map<Stream, StreamMatch> matches, Set<Stream> timeouts) {
        for (String field : fields) {
            for (final Rule rule : rules.get(field)) {
                final Callable<Stream> task = new Callable<Stream>() {
                    @Override
                    public Stream call() {
                        return rule.match(message);
                    }
                };

                try {
                    final Stream match = timeLimiter.callWithTimeout(task, streamProcessingTimeout, TimeUnit.MILLISECONDS, true);

                    registerMatch(matches, match);
                } catch (UncheckedTimeoutException e) {
                    timeouts.add(rule.getStream());
                } catch (Exception e) {
                    LOG.error("Unexpected stream rule exception.", e);
                }
            }
        }
    }

    private void registerMatch(Map<Stream, StreamMatch> matches, Stream match) {
        if (match != null) {
            if (!matches.containsKey(match)) {
                matches.put(match, new StreamMatch(match));
            }
            matches.get(match).increment();
        }
    }

    private void addRule(Map<String, List<Rule>> rules, Set<String> fields, String field, Rule rule) {
        fields.add(field);

        if (! rules.containsKey(field)) {
            rules.put(field, Lists.newArrayList(rule));
        } else {
            rules.get(field).add(rule);
        }
    }

    private class StreamMatch {
        private final int ruleCount;
        private int matches = 0;
        private final Stream stream;

        public StreamMatch(Stream stream) {
            this.stream = stream;
            this.ruleCount = stream.getStreamRules().size();
        }

        public void increment() {
            matches++;
        }

        public boolean isMatched() {
            switch (stream.getMatchingType()) {
                case AND: return ruleCount == matches;
                case OR: return ruleCount > 0;
                default: return ruleCount == matches;
            }
        }
    }

    protected class Rule {
        private final Stream stream;
        private final StreamRule rule;
        private final StreamRuleMatcher matcher;

        public Rule(Stream stream, StreamRule rule) throws InvalidStreamRuleTypeException {
            this.stream = stream;
            this.rule = rule;
            this.matcher = StreamRuleMatcherFactory.build(rule.getType());
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
