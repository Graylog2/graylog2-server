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
package org.graylog2.outputs.filter;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.listeners.NoopInterpreterListener;
import org.graylog2.indexer.messages.ImmutableMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.messageq.noop.NoopMessageQueueAcknowledger;
import org.graylog2.streams.filters.StreamDestinationFilterDeletedEvent;
import org.graylog2.streams.filters.StreamDestinationFilterUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * An {@link OutputFilter} that filters messages with pipeline processor rules based on stream destination filters.
 *
 * @see org.graylog2.streams.filters.StreamDestinationFilterService
 */
@Singleton
public class PipelineRuleOutputFilter implements OutputFilter {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineRuleOutputFilter.class);
    private static final NoopInterpreterListener NOOP_INTERPRETER_LISTENER = new NoopInterpreterListener();
    public static final String METADATA_KEY = PipelineRuleOutputFilter.class.getCanonicalName();

    private final PipelineRuleOutputFilterStateUpdater stateUpdater;
    private final ScheduledExecutorService scheduler;
    private final AtomicReference<PipelineRuleOutputFilterState> activeState = new AtomicReference<>();
    private final PipelineInterpreter pipelineInterpreter;
    private final Timer executionTime;

    @Inject
    public PipelineRuleOutputFilter(PipelineRuleOutputFilterStateUpdater stateUpdater,
                                    @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                    MetricRegistry metricRegistry,
                                    EventBus eventBus) {
        this.stateUpdater = stateUpdater;
        this.scheduler = scheduler;

        // TODO: We probably want to extract the actual pipeline interpreter out of the PipelineInterpreter which
        //       is actually a MessageProcessor. That would make it more reusable.
        // We can pass a null value for the stateUpdater because we don't use functions that use it.
        this.pipelineInterpreter = new PipelineInterpreter(new NoopMessageQueueAcknowledger(), new MetricRegistry(), null);

        this.executionTime = metricRegistry.timer(name(getClass(), "executionTime"));

        eventBus.register(this);
        stateUpdater.init(activeState); // This blocks to ensure we have a config before we start processing
    }

    @SuppressWarnings("unused")
    @Subscribe
    private void handleFilterUpdate(StreamDestinationFilterUpdatedEvent event) {
        LOG.debug("Handling filter update: {}", event);
        scheduler.execute(() -> stateUpdater.reloadForUpdate(activeState, event.ids()));
    }

    @SuppressWarnings("unused")
    @Subscribe
    private void handleFilterDelete(StreamDestinationFilterDeletedEvent event) {
        LOG.debug("Handling filter deletion: {}", event);
        scheduler.execute(() -> stateUpdater.reloadForDelete(activeState, event.ids()));
    }

    @Override
    public FilteredMessage apply(Message msg) {
        try (var ignored = executionTime.time()) {
            return doApply(msg);
        }
    }

    public record Metadata(Multimap<String, Stream> destinations) {
        public static Metadata forDestinationsAndStreams(Set<String> destinations, Set<Stream> streams) {
            final Multimap<String, Stream> destinationsBuilder = MultimapBuilder.hashKeys().hashSetValues().build();

            // Populate each destination with all message streams
            destinations.forEach(destination -> destinationsBuilder.putAll(destination, streams));

            return new Metadata(destinationsBuilder);
        }
    }

    private DefaultFilteredMessage doApply(Message msg) {
        final var state = activeState.get();
        if (state == null) {
            throw new IllegalStateException("Active state has not been initialized");
        }

        final var newMetadata = Metadata.forDestinationsAndStreams(state.getDestinations(), msg.getStreams());

        if (state.isEmpty()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("No active filter - returning early");
            }
            return new DefaultFilteredMessage(ImmutableMessage.wrap(msg), newMetadata.destinations());
        }

        // Add metadata to message, so we can modify it from the filter rules
        msg.setMetadata(METADATA_KEY, newMetadata);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Message metadata before running filter: {} (message-id={})", newMetadata, msg.getId());
        }

        final var pipelinesToRun = state.getPipelinesForMessage(msg);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Running {} pipelines for message <{}>: {}", pipelinesToRun.size(), msg.getId(), pipelinesToRun);
        }
        pipelineInterpreter.processForResolvedPipelines(
                msg,
                msg.getId(),
                pipelinesToRun,
                NOOP_INTERPRETER_LISTENER,
                activeState.get()
        );

        if (msg.getMetadataValue(METADATA_KEY) instanceof Metadata metadata) {
            msg.removeMetadata(METADATA_KEY); // Free up metadata early since we don't need it anymore
            if (LOG.isTraceEnabled()) {
                LOG.trace("Message metadata after running filter: {} (message-id={})", metadata, msg.getId());
            }
            return new DefaultFilteredMessage(ImmutableMessage.wrap(msg), metadata.destinations());
        }
        // Since we set the metadata before we pass the message into the interpreter, it's a bug if it doesn't exist.
        throw new IllegalStateException(f("No metadata found for message <%s> - this should not happen!", msg.getId()));
    }
}
