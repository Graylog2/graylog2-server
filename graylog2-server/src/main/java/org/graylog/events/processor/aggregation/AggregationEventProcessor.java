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
package org.graylog.events.processor.aggregation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventOriginContext;
import org.graylog.events.event.EventReplayInfo;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.processor.DBEventProcessorStateService;
import org.graylog.events.processor.EventConsumer;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessor;
import org.graylog.events.processor.EventProcessorDependencyCheck;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog.events.processor.EventProcessorPreconditionException;
import org.graylog.events.processor.EventStreamService;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.errors.ParameterExpansionError;
import org.graylog.plugins.views.search.errors.SearchException;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.graylog.events.search.MoreSearch.luceneEscape;

public class AggregationEventProcessor implements EventProcessor {
    public interface Factory extends EventProcessor.Factory<AggregationEventProcessor> {
        @Override
        AggregationEventProcessor create(EventDefinition eventDefinition);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AggregationEventProcessor.class);

    private final EventDefinition eventDefinition;
    private final AggregationEventProcessorConfig config;
    private final EventProcessorDependencyCheck dependencyCheck;
    private final DBEventProcessorStateService stateService;
    private final MoreSearch moreSearch;
    private final EventStreamService eventStreamService;
    private final Messages messages;
    private final PermittedStreams permittedStreams;
    private final Set<EventQuerySearchTypeSupplier> eventQueryModifiers;
    private final MessageFactory messageFactory;
    private final AggregationSearchUtils aggregationSearchUtils;

    @Inject
    public AggregationEventProcessor(@Assisted EventDefinition eventDefinition,
                                     AggregationSearch.Factory aggregationSearchFactory,
                                     EventProcessorDependencyCheck dependencyCheck,
                                     DBEventProcessorStateService stateService,
                                     MoreSearch moreSearch,
                                     EventStreamService eventStreamService,
                                     Messages messages, NotificationService notificationService,
                                     PermittedStreams permittedStreams,
                                     Set<EventQuerySearchTypeSupplier> eventQueryModifiers,
                                     MessageFactory messageFactory) {
        this.eventDefinition = eventDefinition;
        this.config = (AggregationEventProcessorConfig) eventDefinition.config();
        this.dependencyCheck = dependencyCheck;
        this.stateService = stateService;
        this.moreSearch = moreSearch;
        this.eventStreamService = eventStreamService;
        this.messages = messages;
        this.permittedStreams = permittedStreams;
        this.eventQueryModifiers = eventQueryModifiers;
        this.messageFactory = messageFactory;
        // If this is a simple Filter search there is no need to initialize aggregationSearchUtils
        this.aggregationSearchUtils = config.series().isEmpty() ? null : new AggregationSearchUtils(
                eventDefinition,
                config,
                eventQueryModifiers,
                aggregationSearchFactory,
                eventStreamService,
                messageFactory,
                permittedStreams
        );
    }

    @Override
    public void createEvents(EventFactory eventFactory, EventProcessorParameters processorParameters, EventConsumer<List<EventWithContext>> eventsConsumer) throws EventProcessorException {
        final AggregationEventProcessorParameters parameters = (AggregationEventProcessorParameters) processorParameters;

        // TODO: We have to take the Elasticsearch index.refresh_interval into account here!
        if (!dependencyCheck.hasMessagesIndexedUpTo(parameters.timerange())) {
            final String msg = String.format(Locale.ROOT, "Couldn't run aggregation <%s/%s> for timerange <%s to %s> because required messages haven't been indexed, yet.",
                    eventDefinition.title(), eventDefinition.id(), parameters.timerange().getFrom(), parameters.timerange().getTo());
            throw new EventProcessorPreconditionException(msg, eventDefinition);
        }

        LOG.debug("Creating events for config={} parameters={}", config, parameters);

        // The absence of a series indicates that the user doesn't want to do an aggregation but create events from
        // a simple search query. (one message -> one event)
        try {
            if (config.series().isEmpty()) {
                filterSearch(eventFactory, parameters, eventsConsumer);
            } else {
                aggregationSearchUtils.aggregatedSearch(eventFactory, parameters, eventsConsumer);
            }
        } catch (SearchException e) {
            if (e.error() instanceof ParameterExpansionError) {
                final String msg = String.format(Locale.ROOT, "Couldn't run aggregation <%s/%s>  because parameters failed to expand: %s",
                        eventDefinition.title(), eventDefinition.id(), e.error().description());
                LOG.error(msg);
                throw new EventProcessorPreconditionException(msg, eventDefinition, e);
            }
        } catch (ElasticsearchException e) {
            final String msg = String.format(Locale.ROOT, "Couldn't run aggregation <%s/%s> because of search error: %s",
                    eventDefinition.title(), eventDefinition.id(), e.getMessage());
            LOG.error(msg);
            throw new EventProcessorPreconditionException(msg, eventDefinition, e);
        }

        // Update the state for this processor! This state will be used for dependency checks between event processors.
        stateService.setState(eventDefinition.id(), parameters.timerange().getFrom(), parameters.timerange().getTo());
    }

    @Override
    public void sourceMessagesForEvent(Event event, Consumer<List<MessageSummary>> messageConsumer, long limit) throws EventProcessorException {
        if (config.series().isEmpty()) {
            if (limit <= 0) {
                return;
            }
            final EventOriginContext.ESEventOriginContext esContext =
                    EventOriginContext.parseESContext(event.getOriginContext()).orElseThrow(
                            () -> new EventProcessorException("Failed to parse origin context", false, eventDefinition));
            try {
                final ResultMessage message;
                message = messages.get(esContext.messageId(), esContext.indexName());
                messageConsumer.accept(Lists.newArrayList(new MessageSummary(message.getIndex(), message.getMessage())));
            } catch (IOException e) {
                throw new EventProcessorException("Failed to query origin context message", false, eventDefinition, e);
            }

        } else {
            final AtomicLong msgCount = new AtomicLong(0L);
            final MoreSearch.ScrollCallback callback = (messages, continueScrolling) -> {
                final List<MessageSummary> summaries = Lists.newArrayList();
                for (final ResultMessage resultMessage : messages) {
                    if (msgCount.incrementAndGet() > limit) {
                        continueScrolling.set(false);
                        break;
                    }
                    final Message msg = resultMessage.getMessage();
                    summaries.add(new MessageSummary(resultMessage.getIndex(), msg));
                }
                messageConsumer.accept(summaries);
            };

            ElasticsearchQueryString scrollQueryString = ElasticsearchQueryString.of(config.query());
            scrollQueryString = scrollQueryString.concatenate(groupByQueryString(event));
            LOG.debug("scrollQueryString: {}", scrollQueryString);

            final TimeRange timeRange = AbsoluteRange.create(event.getTimerangeStart(), event.getTimerangeEnd());
            moreSearch.scrollQuery(scrollQueryString.queryString(), config.streams(), config.filters(),
                    config.queryParameters(), timeRange, Math.min(500, Ints.saturatedCast(limit)), callback);
        }
    }

    // Return the ES query string for the group by fields specified in event; or empty if none specified.
    // Search value is escaped and enclosed in quotes.
    private ElasticsearchQueryString groupByQueryString(Event event) {
        ElasticsearchQueryString result = ElasticsearchQueryString.empty();
        if (!config.groupBy().isEmpty()) {
            for (String key : event.getGroupByFields().keySet()) {
                String value = event.getGroupByFields().get(key);
                String query = new StringBuilder(key).append(":\"").append(luceneEscape(value)).append("\"").toString();
                result = result.concatenate(ElasticsearchQueryString.of(query));
            }
        }
        return result;
    }

    /**
     * This returns the actual streams set based on the given parameters and the event definition.
     * Streams in parameters will override the ones in the config.
     *
     * @param parameters aggregation event processor parameters
     * @return the actual streams
     */
    private Set<String> getStreams(AggregationEventProcessorParameters parameters) {
        if (parameters.streams().isEmpty()) {
            Set<String> configStreams = new HashSet<>(config.streams());
            if (!config.streamCategories().isEmpty()) {
                // TODO: We need to account for permissions of the user who created the event here in place of
                //      a blanket `true` here.
                configStreams.addAll(permittedStreams.loadWithCategories(config.streamCategories(), streamId -> true));
            }
            return configStreams;
        } else {
            return parameters.streams();
        }
    }

    private void filterSearch(EventFactory eventFactory, AggregationEventProcessorParameters parameters,
                              EventConsumer<List<EventWithContext>> eventsConsumer) throws EventProcessorException {
        Set<String> streams = getStreams(parameters);
        if (streams.isEmpty()) {
            streams = new HashSet<>(permittedStreams.loadAllMessageStreams(streamId -> true));
        }

        final AtomicInteger messageCount = new AtomicInteger(0);
        final MoreSearch.ScrollCallback callback = (messages, continueScrolling) -> {
            final ImmutableList.Builder<EventWithContext> eventsWithContext = ImmutableList.builder();

            for (final ResultMessage resultMessage : messages) {
                final Message msg = resultMessage.getMessage();
                final Event event = eventFactory.createEvent(eventDefinition, msg.getTimestamp(), eventDefinition.title());
                event.setOriginContext(EventOriginContext.elasticsearchMessage(resultMessage.getIndex(), msg.getId()));

                // Ensure the event has values in the "source_streams" field for permission checks to work
                eventStreamService.buildEventSourceStreams(getStreams(parameters), ImmutableSet.copyOf(msg.getStreamIds()))
                        .forEach(event::addSourceStream);

                event.setReplayInfo(EventReplayInfo.builder()
                        .timerangeStart(parameters.timerange().getFrom())
                        .timerangeEnd(parameters.timerange().getTo())
                        .query(config.query())
                        .streams(event.getSourceStreams())
                        .filters(config.filters())
                        .build());

                eventsWithContext.add(EventWithContext.create(event, msg));
                if (config.eventLimit() != 0) {
                    if (messageCount.incrementAndGet() >= config.eventLimit()) {
                        eventsConsumer.accept(eventsWithContext.build());
                        throw new EventLimitReachedException();
                    }
                }
            }
            eventsConsumer.accept(eventsWithContext.build());
        };

        try {
            moreSearch.scrollQuery(config.query(), streams, config.filters(), config.queryParameters(),
                    parameters.timerange(), parameters.batchSize(), callback);
        } catch (EventLimitReachedException e) {
            LOG.debug("Event limit reached at {} for '{}/{}' event definition.", config.eventLimit(), eventDefinition.title(), eventDefinition.id());
        }
    }

    private static class EventLimitReachedException extends RuntimeException {
    }
}
