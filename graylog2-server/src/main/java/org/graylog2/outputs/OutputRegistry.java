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
package org.graylog2.outputs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import org.graylog2.database.NotFoundException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.outputs.events.OutputChangedEvent;
import org.graylog2.plugin.outputs.InsufficientLicenseException;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Singleton
public class OutputRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(OutputRegistry.class);
    private static final Logger RATE_LIMITED_LOG = RateLimitedLog.withRateLimit(LOG)
            .maxRate(1)
            .every(Duration.ofSeconds(5))
            .build();

    private final Cache<String, MessageOutput> runningMessageOutputs;
    private final MessageOutput defaultMessageOutput;
    private final OutputService outputService;
    private final NotificationService notificationService;
    private final NodeId nodeId;
    private final MessageOutputFactory messageOutputFactory;
    private final LoadingCache<String, AtomicInteger> faultCounters;
    private final StreamService streamService;
    private final long faultCountThreshold;
    private final long faultPenaltySeconds;

    @Inject
    public OutputRegistry(@DefaultMessageOutput MessageOutput defaultMessageOutput,
                          OutputService outputService,
                          MessageOutputFactory messageOutputFactory,
                          NotificationService notificationService,
                          NodeId nodeId,
                          EventBus eventBus,
                          StreamService streamService,
                          @Named("output_fault_count_threshold") long faultCountThreshold,
                          @Named("output_fault_penalty_seconds") long faultPenaltySeconds) {
        this.defaultMessageOutput = defaultMessageOutput;
        this.outputService = outputService;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
        this.messageOutputFactory = messageOutputFactory;
        this.streamService = streamService;
        this.runningMessageOutputs = CacheBuilder.newBuilder().build();
        this.faultCountThreshold = faultCountThreshold;
        this.faultPenaltySeconds = faultPenaltySeconds;
        this.faultCounters = CacheBuilder.newBuilder()
                .expireAfterWrite(this.faultPenaltySeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Override
                    public AtomicInteger load(String key) throws Exception {
                        return new AtomicInteger(0);
                    }
                });

        eventBus.register(this);
    }

    /**
     * Stops the output and removes it from the registry.
     * It will be restarted automatically by the {@link OutputRouter}
     * via {@link #getOutputForIdAndStream(String, Stream)}
     */
    @Subscribe
    public void handleOutputChanged(OutputChangedEvent outputChangedEvent) {
        removeOutput(outputChangedEvent.outputId());
    }

    /**
     * Whenever a stream changes, this could mean that an output was removed from it. Checks which outputs
     * are currently assigned to any streams and removes those from the registry which are not expected to be
     * running.
     */
    @Subscribe
    public void handleStreamsChanged(StreamsChangedEvent streamsChangedEvent) {
        if (streamsChangedEvent.streamIds().isEmpty()) {
            return;
        }

        final Set<String> expectedRunningOutputs = streamService.loadAllEnabled().stream()
                .flatMap(stream -> stream.getOutputs().stream()).map(Output::getId).collect(Collectors.toSet());
        final Set<String> currentlyRunningOutputs = runningMessageOutputs.asMap().keySet();

        Sets.difference(currentlyRunningOutputs, expectedRunningOutputs).forEach(this::removeOutput);
    }

    @Nullable
    public MessageOutput getOutputForIdAndStream(String id, Stream stream) {
        final AtomicInteger faultCount;
        try {
            faultCount = this.faultCounters.get(id);
        } catch (ExecutionException e) {
            LOG.error("Unable to retrieve output fault counter", e);
            return null;
        }

        try {
            if (faultCount.get() < faultCountThreshold) {
                return this.runningMessageOutputs.get(id, loadForIdAndStream(id, stream));
            }
        } catch (ExecutionException | UncheckedExecutionException e) {
            if (e.getCause() instanceof NotFoundException || e.getCause() instanceof IllegalArgumentException) {
                RATE_LIMITED_LOG.debug("Unable to fetch output <{}> for stream <{}/{}>: {}", id, stream.getTitle(), stream.getId(), e.getMessage());
            } else {
                final int number = faultCount.addAndGet(1);
                if (e.getCause() instanceof InsufficientLicenseException licenseException) {
                    LOG.error("Unable to fetch output {}, fault #{}: {}", id, number,
                            licenseException.getLocalizedMessage());
                } else {
                    LOG.error("Unable to fetch output " + id + ", fault #" + number, e);
                }
                if (number >= faultCountThreshold) {
                    LOG.error("Output {} has crossed threshold of {} faults in {} seconds. Disabling for {} seconds.",
                            id,
                            faultCountThreshold,
                            faultPenaltySeconds,
                            faultPenaltySeconds);

                    final Notification notification = notificationService.buildNow()
                            .addType(Notification.Type.OUTPUT_DISABLED)
                            .addSeverity(Notification.Severity.NORMAL)
                            .addNode(nodeId.getNodeId())
                            .addDetail("outputId", id)
                            .addDetail("streamId", stream.getId())
                            .addDetail("streamTitle", stream.getTitle())
                            .addDetail("faultCount", number)
                            .addDetail("faultCountThreshold", faultCountThreshold)
                            .addDetail("faultPenaltySeconds", faultPenaltySeconds);
                    notificationService.publishIfFirst(notification);
                }
            }
        }
        return null;
    }

    public Callable<MessageOutput> loadForIdAndStream(final String id, final Stream stream) {
        return new Callable<>() {
            @Override
            public MessageOutput call() throws Exception {
                // Check if the output is still assigned to the given stream before loading and starting it.
                // The stream assignment of the output could have been removed while the message object went
                // through processing and output buffer processing handling.
                // Without this check, we would start the output again after it has been stopped by removing it
                // from a stream.
                final Stream dbStream = streamService.load(stream.getId());
                if (dbStream.getOutputs().stream().map(Output::getId).anyMatch(id::equalsIgnoreCase)) {
                    final Output output = outputService.load(id);
                    return launchOutput(output, stream);
                }
                throw new IllegalArgumentException("Output not assigned to stream");
            }
        };
    }

    protected MessageOutput launchOutput(Output output, Stream stream) throws Exception {
        final MessageOutput messageOutput = messageOutputFactory.fromStreamOutput(output, stream,
                new org.graylog2.plugin.configuration.Configuration(output.getConfiguration()));

        if (messageOutput == null) {
            throw new IllegalArgumentException("Failed to instantiate MessageOutput from Output: " + output);
        }

        messageOutput.initialize();

        return messageOutput;
    }

    @VisibleForTesting
    protected Map<String, MessageOutput> getRunningMessageOutputs() {
        return ImmutableMap.copyOf(runningMessageOutputs.asMap());
    }

    public Set<MessageOutput> getMessageOutputs() {
        return ImmutableSet.<MessageOutput>builder()
                .addAll(runningMessageOutputs.asMap().values())
                .add(defaultMessageOutput)
                .build();
    }

    public void removeOutput(Output output) {
        removeOutput(output.getId());
    }

    private void removeOutput(String outputId) {
        final MessageOutput messageOutput = runningMessageOutputs.getIfPresent(outputId);
        if (messageOutput != null) {
            messageOutput.stop();
        }

        runningMessageOutputs.invalidate(outputId);
        faultCounters.invalidate(outputId);
    }
}
