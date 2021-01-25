/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.streams;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.DefaultStream;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.matchers.StreamRuleMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.EnumSet;
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

    private final EnumSet<StreamRuleType> ruleTypesNotNeedingFieldPresence = EnumSet.of(StreamRuleType.PRESENCE, StreamRuleType.EXACT, StreamRuleType.REGEX, StreamRuleType.ALWAYS_MATCH, StreamRuleType.CONTAINS, StreamRuleType.MATCH_INPUT);
    private final List<Stream> streams;
    private final StreamFaultManager streamFaultManager;
    private final StreamMetrics streamMetrics;
    private final TimeLimiter timeLimiter;
    private final long streamProcessingTimeout;
    private final String fingerprint;
    private final Provider<Stream> defaultStreamProvider;

    private final List<Rule> rulesList;

    public interface Factory {
        StreamRouterEngine create(List<Stream> streams, ExecutorService executorService);
    }

    @Inject
    public StreamRouterEngine(@Assisted List<Stream> streams,
                              @Assisted ExecutorService executorService,
                              StreamFaultManager streamFaultManager,
                              StreamMetrics streamMetrics,
                              @DefaultStream Provider<Stream> defaultStreamProvider) {
        this.streams = streams;
        this.streamFaultManager = streamFaultManager;
        this.streamMetrics = streamMetrics;
        this.timeLimiter = SimpleTimeLimiter.create(executorService);
        this.streamProcessingTimeout = streamFaultManager.getStreamProcessingTimeout();
        this.fingerprint = new StreamListFingerprint(streams).getFingerprint();
        this.defaultStreamProvider = defaultStreamProvider;

        final List<Rule> alwaysMatchRules = Lists.newArrayList();
        final List<Rule> presenceRules = Lists.newArrayList();
        final List<Rule> exactRules = Lists.newArrayList();
        final List<Rule> greaterRules = Lists.newArrayList();
        final List<Rule> smallerRules = Lists.newArrayList();
        final List<Rule> regexRules = Lists.newArrayList();
        final List<Rule> containsRules = Lists.newArrayList();
        final List<Rule> matchInputRules = Lists.newArrayList();

        for (Stream stream : streams) {
            for (StreamRule streamRule : stream.getStreamRules()) {
                final Rule rule;
                try {
                    rule = new Rule(stream, streamRule, stream.getMatchingType());
                } catch (InvalidStreamRuleTypeException e) {
                    LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
                    continue;
                }
                switch (streamRule.getType()) {
                    case ALWAYS_MATCH:
                        alwaysMatchRules.add(rule);
                        break;
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
                    case CONTAINS:
                        containsRules.add(rule);
                        break;
                    case MATCH_INPUT:
                        matchInputRules.add(rule);
                        break;
                }
            }
        }

        final int size = alwaysMatchRules.size() + presenceRules.size() + exactRules.size() + greaterRules.size() + smallerRules.size() + containsRules.size() + regexRules.size() + matchInputRules.size();
        this.rulesList = Lists.newArrayListWithCapacity(size);
        this.rulesList.addAll(alwaysMatchRules);
        this.rulesList.addAll(presenceRules);
        this.rulesList.addAll(exactRules);
        this.rulesList.addAll(matchInputRules);
        this.rulesList.addAll(greaterRules);
        this.rulesList.addAll(smallerRules);
        this.rulesList.addAll(containsRules);
        this.rulesList.addAll(regexRules);
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
        final Set<String> blackList = Sets.newHashSet();

        for (final Rule rule : rulesList) {
            if (blackList.contains(rule.getStreamId())) {
                continue;
            }

            final StreamRule streamRule = rule.getStreamRule();
            final StreamRuleType streamRuleType = streamRule.getType();
            final Stream.MatchingType matchingType = rule.getMatchingType();
            if (!ruleTypesNotNeedingFieldPresence.contains(streamRuleType)
                && !message.hasField(streamRule.getField())) {
                if (matchingType == Stream.MatchingType.AND) {
                    result.remove(rule.getStream());
                    // blacklist stream because it can't match anymore
                    blackList.add(rule.getStreamId());
                }

                continue;
            }

            final Stream stream;
            if (streamRuleType != StreamRuleType.REGEX) {
                stream = rule.match(message);
            } else {
                stream = rule.matchWithTimeOut(message, streamProcessingTimeout, TimeUnit.MILLISECONDS);
            }

            if (stream == null) {
                if (matchingType == Stream.MatchingType.AND) {
                    result.remove(rule.getStream());
                    // blacklist stream because it can't match anymore
                    blackList.add(rule.getStreamId());
                }
            } else {
                result.add(stream);
                if (matchingType == Stream.MatchingType.OR) {
                    // blacklist stream because it is already matched
                    blackList.add(rule.getStreamId());
                }
            }
        }

        final Stream defaultStream = defaultStreamProvider.get();
        boolean alreadyRemovedDefaultStream = false;
        for (Stream stream : result) {
            streamMetrics.markIncomingMeter(stream.getId());
            if (stream.getRemoveMatchesFromDefaultStream()) {
                if (alreadyRemovedDefaultStream || message.removeStream(defaultStream)) {
                    alreadyRemovedDefaultStream = true;
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Successfully removed default stream <{}> from message <{}>", defaultStream.getId(), message.getId());
                    }
                } else {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Couldn't remove default stream <{}> from message <{}>", defaultStream.getId(), message.getId());
                    }
                }
            }
        }
        // either the message stayed on the default stream, in which case we mark that stream's throughput,
        // or someone removed it, in which case we don't mark it.
        if (!alreadyRemovedDefaultStream) {
            streamMetrics.markIncomingMeter(defaultStream.getId());
        }

        return ImmutableList.copyOf(result);
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
                    final Rule rule = new Rule(stream, streamRule, stream.getMatchingType());
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
        private final String streamId;
        private final String streamRuleId;
        private final StreamRuleMatcher matcher;
        private final Stream.MatchingType matchingType;

        public Rule(Stream stream, StreamRule rule, Stream.MatchingType matchingType) throws InvalidStreamRuleTypeException {
            this.stream = stream;
            this.rule = rule;
            this.streamId = stream.getId();
            this.streamRuleId = rule.getId();
            this.matchingType = matchingType;
            this.matcher = StreamRuleMatcherFactory.build(rule.getType());
        }

        public Stream.MatchingType getMatchingType() {
            return matchingType;
        }

        @Nullable
        public Stream match(Message message) {
            // TODO Add missing message recordings!
            try (final Timer.Context ignored = streamMetrics.getExecutionTimer(streamId, streamRuleId).time()) {
                if (matcher.match(message, rule)) {
                    return stream;
                } else {
                    return null;
                }
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Error matching stream rule <" + rule.getType() + "/" + rule.getValue() + ">: " + e.getMessage(), e);
                }
                streamMetrics.markExceptionMeter(streamId);
                return null;
            }
        }

        @Nullable
        private Stream matchWithTimeOut(final Message message, long timeout, TimeUnit unit) {
            Stream matchedStream = null;
            try (final Timer.Context ignored = streamMetrics.getExecutionTimer(streamId, streamRuleId).time()) {
                matchedStream = timeLimiter.callWithTimeout(new Callable<Stream>() {
                    @Override
                    @Nullable
                    public Stream call() throws Exception {
                        return match(message);
                    }
                }, timeout, unit);
            } catch (UncheckedTimeoutException e) {
                streamFaultManager.registerFailure(stream);
            } catch (Exception e) {
                LOG.warn("Unexpected error during stream matching", e);
                streamMetrics.markExceptionMeter(streamId);
            }

            return matchedStream;
        }

        public StreamRule getStreamRule() {
            return rule;
        }

        public Stream getStream() {
            return stream;
        }

        public String getStreamId() {
            return streamId;
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
                matches.put(rule.getStreamRule(), match != null && match.equals(stream));
            }
        }

        public boolean isMatched() {
            switch (matchingType) {
                case OR:
                    return matches.values().contains(true);
                case AND:
                default:
                    return matches.size() > 0 && !matches.values().contains(false);
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
