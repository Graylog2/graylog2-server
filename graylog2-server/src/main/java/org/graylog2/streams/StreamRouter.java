/**
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
 */
package org.graylog2.streams;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import org.graylog2.Configuration;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.streams.matchers.StreamRuleMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Routes a GELF message to its streams.
 */
public class StreamRouter {
    private static final Logger LOG = LoggerFactory.getLogger(StreamRouter.class);

    private final Map<String, Meter> streamIncomingMeters = Maps.newHashMap();
    private final Map<String, Timer> streamExecutionTimers = Maps.newHashMap();
    private final Map<String, Meter> streamExceptionMeters = Maps.newHashMap();
    private final Map<String, Meter> streamRuleTimeoutMeters = Maps.newHashMap();
    private final Map<String, Meter> streamFaultsExceededMeters = Maps.newHashMap();

    protected final StreamService streamService;
    protected final StreamRuleService streamRuleService;
    private final MetricRegistry metricRegistry;
    private final Configuration configuration;
    private final NotificationService notificationService;

    private final ExecutorService executor;
    private final TimeLimiter timeLimiter;

    final private ConcurrentMap<String, AtomicInteger> faultCounter;

    @Inject
    public StreamRouter(StreamService streamService,
                        StreamRuleService streamRuleService,
                        MetricRegistry metricRegistry,
                        Configuration configuration,
                        NotificationService notificationService) {
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.metricRegistry = metricRegistry;
        this.configuration = configuration;
        this.notificationService = notificationService;
        this.faultCounter = Maps.newConcurrentMap();
        this.executor = executorService();
        this.timeLimiter = new SimpleTimeLimiter(executor);
    }

    private ExecutorService executorService() {
        return new InstrumentedExecutorService(Executors.newCachedThreadPool(threadFactory()), metricRegistry);
    }

    private ThreadFactory threadFactory() {
        return new InstrumentedThreadFactory(new ThreadFactoryBuilder()
                .setNameFormat("stream-router-%d")
                .setDaemon(true)
                .build(), metricRegistry);
    }

    private AtomicInteger getFaultCount(String streamId) {
        faultCounter.putIfAbsent(streamId, new AtomicInteger());
        return faultCounter.get(streamId);
    }

    public List<Stream> route(final Message msg) {
        List<Stream> matches = Lists.newArrayList();
        List<Stream> streams = getStreams();

        final long timeout = configuration.getStreamProcessingTimeout();
        final int maxFaultCount = configuration.getStreamProcessingMaxFaults();

        for (final Stream stream : streams) {
            final Timer timer = getExecutionTimer(stream.getId());

            Callable<Boolean> task = new Callable<Boolean>() {
                public Boolean call() {
                    Map<StreamRule, Boolean> result = getRuleMatches(stream, msg);
                    return doesStreamMatch(result);
                }
            };

            try (final Timer.Context timerContext = timer.time()) {
                boolean matched = timeLimiter.callWithTimeout(task, timeout, TimeUnit.MILLISECONDS, true);
                if (matched) {
                    getIncomingMeter(stream.getId()).mark();
                    matches.add(stream);
                }
            } catch (Exception e) {
                AtomicInteger faultCount = getFaultCount(stream.getId());
                int streamFaultCount = faultCount.incrementAndGet();
                getStreamRuleTimeoutMeter(stream.getId()).mark();
                if (maxFaultCount > 0 && streamFaultCount >= maxFaultCount) {
                    try {
                        streamService.pause(stream);
                        faultCount.set(0);
                        getStreamFaultsExceededMeter(stream.getId()).mark();
                        LOG.error("Processing of stream <" + stream.getId() + "> failed to return within " + timeout + "ms for more than " + maxFaultCount + " times. Disabling stream.");

                        Notification notification = notificationService.buildNow()
                                .addType(Notification.Type.STREAM_PROCESSING_DISABLED)
                                .addSeverity(Notification.Severity.URGENT)
                                .addDetail("stream_id", stream.getId())
                                .addDetail("fault_count", streamFaultCount);
                        notificationService.publishIfFirst(notification);
                    } catch (ValidationException ex) {
                        LOG.error("Unable to pause stream: {}", ex);
                    }
                } else
                    LOG.warn("Processing of stream <{}> failed to return within {}ms.", stream.getId(), timeout);
            }
        }

        return matches;
    }

    List<Stream> getStreams() {
        return streamService.loadAllEnabled();
    }

    List<StreamRule> getStreamRules(Stream stream) {
        try {
            return streamRuleService.loadForStream(stream);
        } catch (NotFoundException e) {
            LOG.error("Caught exception while fetching stream rules", e);
            return Collections.emptyList();
        }
    }

    public Map<StreamRule, Boolean> getRuleMatches(Stream stream, Message msg) {
        Map<StreamRule, Boolean> result = Maps.newHashMap();

        List<StreamRule> streamRules = getStreamRules(stream);

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
            LOG.debug("Could not match stream rule <" + rule.getType() + "/" + rule.getValue() + ">: " + e.getMessage(), e);
            getExceptionMeter(rule.getStreamId()).mark();
            return false;
        }
    }

    protected Meter getIncomingMeter(String streamId) {
        Meter meter = this.streamIncomingMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "incomingMessages"));
            this.streamIncomingMeters.put(streamId, meter);
        }

        return meter;
    }

    protected Timer getExecutionTimer(String streamId) {
        Timer timer = this.streamExecutionTimers.get(streamId);
        if (timer == null) {
            timer = metricRegistry.timer(MetricRegistry.name(Stream.class, streamId, "executionTime"));
            this.streamExecutionTimers.put(streamId, timer);
        }

        return timer;
    }

    protected Meter getExceptionMeter(String streamId) {
        Meter meter = this.streamExceptionMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "matchingExceptions"));
            this.streamExceptionMeters.put(streamId, meter);
        }

        return meter;
    }

    protected Meter getStreamRuleTimeoutMeter(String streamId) {
        Meter meter = this.streamRuleTimeoutMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "ruleTimeouts"));
            this.streamRuleTimeoutMeters.put(streamId, meter);
        }

        return meter;
    }

    protected Meter getStreamFaultsExceededMeter(String streamId) {
        Meter meter = this.streamFaultsExceededMeters.get(streamId);
        if (meter == null) {
            meter = metricRegistry.meter(MetricRegistry.name(Stream.class, streamId, "faultsExceeded"));
            this.streamFaultsExceededMeters.put(streamId, meter);
        }

        return meter;
    }
}
