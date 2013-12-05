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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.GraylogServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.graylog2.streams.matchers.StreamRuleMatcher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
    private static LoadingCache<String, List<Stream>> cachedStreams;
    private static LoadingCache<ObjectId, List<StreamRule>> cachedStreamRules;

    private final Map<String, Meter> streamIncomingMeters = Maps.newHashMap();
    private final Map<String, Timer> streamExecutionTimers = Maps.newHashMap();

    private Boolean useCaching = false;

    public StreamRouter() {
        this(false);
    }

    public StreamRouter(Boolean useCaching) {
        this.useCaching = useCaching;
    }

    public List<Stream> route(Core server, Message msg) {
        List<Stream> matches = Lists.newArrayList();
        List<Stream> streams = getStreams(server);

        for (Stream stream : streams) {
            Timer timer = getExecutionTimer(stream.getId().toStringMongod(), server);
            final Timer.Context timerContext = timer.time();

            Map<StreamRule, Boolean> result = getRuleMatches(server, stream, msg);

            boolean matched = doesStreamMatch(result);

            // All rules were matched.
            if (matched) {
                getIncomingMeter(stream.getId().toStringMongod(), server).mark();
                matches.add(stream);
            }
            timerContext.close();
        }

        return matches;
    }

    private List<Stream> getStreams(final Core server) {
        if (this.useCaching) {
            if (cachedStreams == null)
                cachedStreams = CacheBuilder.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(1, TimeUnit.SECONDS)
                        .build(
                                new CacheLoader<String, List<Stream>>() {
                                    @Override
                                    public List<Stream> load(String s) throws Exception {
                                        return StreamImpl.loadAllEnabled(server);
                                    }
                                }
                        );
            List<Stream> result = null;
            try {
                result = cachedStreams.get("streams");
            } catch (ExecutionException e) {
                LOG.error("Caught exception while fetching from cache", e);
            }
            return result;
        } else {
            return StreamImpl.loadAllEnabled(server);
        }
    }

    private List<StreamRule> getStreamRules(ObjectId streamId, final Core server) {
        if (this.useCaching) {
            if (cachedStreamRules == null)
                cachedStreamRules = CacheBuilder.newBuilder()
                        .expireAfterWrite(1, TimeUnit.SECONDS)
                        .build(
                                new CacheLoader<ObjectId, List<StreamRule>>() {
                                    @Override
                                    public List<StreamRule> load(ObjectId s) throws Exception {
                                        return StreamRuleImpl.findAllForStream(s, server);
                                    }
                                }
                        );
            List<StreamRule> result = null;
            try {
                result = cachedStreamRules.get(streamId);
            } catch (ExecutionException e) {
                LOG.error("Caught exception while fetching from cache", e);
            }

            return result;
        } else {
            try {
                return StreamRuleImpl.findAllForStream(streamId, server);
            } catch (NotFoundException e) {
                LOG.error("Caught exception while fetching stream rules", e);
                return null;
            }
        }
    }

    public Map<StreamRule, Boolean> getRuleMatches(final Core server, Stream stream, Message msg) {
        Map<StreamRule, Boolean> result = Maps.newHashMap();

        List<StreamRule> streamRules = getStreamRules(stream.getId(), server);

        for (StreamRule rule : streamRules) {
            try {
                StreamRuleMatcher matcher = StreamRuleMatcherFactory.build(rule.getType());
                result.put(rule, matchStreamRule(msg, matcher, rule));
            } catch (InvalidStreamRuleTypeException e) {
                LOG.warn("Invalid stream rule type. Skipping matching for this rule. " + e.getMessage(), e);
            }
        }

        return result;
    }

    public boolean doesStreamMatch(Map<StreamRule, Boolean> ruleMatches) {
        return !ruleMatches.isEmpty() && !ruleMatches.values().contains(false);
    }

    public boolean matchStreamRule(Message msg, StreamRuleMatcher matcher, StreamRule rule) {
        try {
            return matcher.match(msg, rule);
        } catch (Exception e) {
            LOG.warn("Could not match stream rule <" + rule.getType() + "/" + rule.getValue() + ">: " + e.getMessage(), e);
            return false;
        }
    }

    protected Meter getIncomingMeter(String streamId, GraylogServer server) {
        Meter meter = this.streamIncomingMeters.get(streamId);
        if (meter == null) {
            meter = server.metrics().meter(MetricRegistry.name(Stream.class, streamId, "incomingMessages"));
            this.streamIncomingMeters.put(streamId, meter);
        }

        return meter;
    }

    protected Timer getExecutionTimer(String streamId, GraylogServer server) {
        Timer timer = this.streamExecutionTimers.get(streamId);
        if (timer == null) {
            timer = server.metrics().timer(MetricRegistry.name(Stream.class, streamId, "executionTime"));
            this.streamExecutionTimers.put(streamId, timer);
        }

        return timer;
    }

}