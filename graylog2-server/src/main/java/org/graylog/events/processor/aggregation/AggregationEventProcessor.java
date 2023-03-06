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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import com.google.inject.assistedinject.Assisted;
import org.apache.logging.log4j.util.Strings;
import org.graylog.events.conditions.BooleanNumberConditionsVisitor;
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
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.graylog.events.search.MoreSearch.luceneEscape;

public class AggregationEventProcessor implements EventProcessor {
    public interface Factory extends EventProcessor.Factory<AggregationEventProcessor> {
        @Override
        AggregationEventProcessor create(EventDefinition eventDefinition);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AggregationEventProcessor.class);

    private final EventDefinition eventDefinition;
    private final AggregationEventProcessorConfig config;
    private final AggregationSearch.Factory aggregationSearchFactory;
    private final EventProcessorDependencyCheck dependencyCheck;
    private final DBEventProcessorStateService stateService;
    private final MoreSearch moreSearch;
    private final EventStreamService eventStreamService;
    private final Messages messages;

    @Inject
    public AggregationEventProcessor(@Assisted EventDefinition eventDefinition,
                                     AggregationSearch.Factory aggregationSearchFactory,
                                     EventProcessorDependencyCheck dependencyCheck,
                                     DBEventProcessorStateService stateService,
                                     MoreSearch moreSearch,
                                     EventStreamService eventStreamService,
                                     Messages messages) {
        this.eventDefinition = eventDefinition;
        this.config = (AggregationEventProcessorConfig) eventDefinition.config();
        this.aggregationSearchFactory = aggregationSearchFactory;
        this.dependencyCheck = dependencyCheck;
        this.stateService = stateService;
        this.moreSearch = moreSearch;
        this.eventStreamService = eventStreamService;
        this.messages = messages;
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
                aggregatedSearch(eventFactory, parameters, eventsConsumer);
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
            moreSearch.scrollQuery(scrollQueryString.queryString(), config.streams(), config.queryParameters(), timeRange, Math.min(500, Ints.saturatedCast(limit)), callback);
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
        return parameters.streams().isEmpty() ? config.streams() : parameters.streams();
    }

    private void filterSearch(EventFactory eventFactory, AggregationEventProcessorParameters parameters,
                              EventConsumer<List<EventWithContext>> eventsConsumer) throws EventProcessorException {
        final Set<String> streams = getStreams(parameters);

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
                        .build());

                eventsWithContext.add(EventWithContext.create(event, msg));
            }

            eventsConsumer.accept(eventsWithContext.build());
        };

        moreSearch.scrollQuery(config.query(), streams, config.queryParameters(), parameters.timerange(), parameters.batchSize(), callback);
    }

    private void aggregatedSearch(EventFactory eventFactory, AggregationEventProcessorParameters parameters,
                                  EventConsumer<List<EventWithContext>> eventsConsumer) throws EventProcessorException {
        final String owner = "event-processor-" + AggregationEventProcessorConfig.TYPE_NAME + "-" + eventDefinition.id();
        final AggregationSearch search = aggregationSearchFactory.create(config, parameters, owner, eventDefinition);
        final AggregationResult result = search.doSearch();

        if (result.keyResults().isEmpty()) {
            LOG.debug("Aggregated search returned empty result set.");
            return;
        }

        LOG.debug("Got {} (total-aggregated-messages={}) results.", result.keyResults().size(), result.totalAggregatedMessages());

        eventsConsumer.accept(eventsFromAggregationResult(eventFactory, parameters, result));
    }

    private boolean satisfiesConditions(AggregationKeyResult keyResult) {
        // We create a map of series IDs to the series value. This will be referenced in the conditions.
        final ImmutableMap<String, Double> numberReferences = keyResult.seriesValues().stream()
                .map(seriesValue -> Maps.immutableEntry(seriesValue.series().id(), seriesValue.value()))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        // Creates a new interpreter and runs the conditions against the referenced values
        if (config.conditions().isPresent()) {
            return config.conditions()
                    .get()
                    .expression()
                    .map(expr -> expr.accept(new BooleanNumberConditionsVisitor(numberReferences)))
                    .orElse(true);
        } else {
            return true;
        }
    }

    @VisibleForTesting
    ImmutableList<EventWithContext> eventsFromAggregationResult(EventFactory eventFactory, AggregationEventProcessorParameters parameters, AggregationResult result)
            throws EventProcessorException {
        final ImmutableList.Builder<EventWithContext> eventsWithContext = ImmutableList.builder();
        final Set<String> sourceStreams = eventStreamService.buildEventSourceStreams(getStreams(parameters),
                result.sourceStreams());

        for (final AggregationKeyResult keyResult : result.keyResults()) {
            if (!satisfiesConditions(keyResult)) {
                LOG.debug("Skipping result <{}> because the conditions <{}> don't match", keyResult, config.conditions());
                continue;
            }

            final String keyString = Strings.join(keyResult.key(), '|');
            final String eventMessage = createEventMessageString(keyString, keyResult);

            // Extract event time and range from the key result or use query time range as fallback.
            // These can be different, e.g. during catch up processing.
            final DateTime eventTime = keyResult.timestamp().orElse(result.effectiveTimerange().to());
            final Event event = eventFactory.createEvent(eventDefinition, eventTime, eventMessage);
            // The keyResult timestamp is set to the end of the range
            event.setTimerangeStart(keyResult.timestamp().map(t -> t.minus(config.searchWithinMs())).orElse(parameters.timerange().getFrom()));
            event.setTimerangeEnd(keyResult.timestamp().orElse(parameters.timerange().getTo()));

            event.setReplayInfo(EventReplayInfo.builder()
                    .timerangeStart(event.getTimerangeStart())
                    .timerangeEnd(event.getTimerangeEnd())
                    .query(config.query())
                    .streams(sourceStreams)
                    .build());
            sourceStreams.forEach(event::addSourceStream);

            final Map<String, Object> fields = new HashMap<>();

            // Each group value will be a separate field in the message to make it usable as event fields.
            //
            // Example result:
            //   groupBy=["application_name", "username"]
            //   result-key=["sshd", "jane"]
            //
            // Message fields:
            //   application_name=sshd
            //   username=jane
            for (int i = 0; i < config.groupBy().size(); i++) {
                try {
                    fields.put(config.groupBy().get(i), keyResult.key().get(i));
                } catch (IndexOutOfBoundsException e) {
                    throw new EventProcessorException(
                            "Couldn't create events for: " + eventDefinition.title() + " (possibly due to non-existing grouping fields)",
                            false, eventDefinition.id(), eventDefinition, e);
                }
            }

            // Group By fields need to be saved on the event so they are available to the subsequent notification events
            event.setGroupByFields(fields.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));

            // The field name for the series value is composed of the series function and field. We don't take the
            // series ID into account because it would be very hard to use for the user. That means a series with
            // the same function and field but different ID would overwrite a previous one.
            // This shouldn't be a problem though, because the same function and field will always compute the same
            // value.
            //
            // Examples:
            //   aggregation_value_count_source=42
            //   aggregation_value_card_anonid=23
            for (AggregationSeriesValue seriesValue : keyResult.seriesValues()) {
                final String function = seriesValue.series().function().toString().toLowerCase(Locale.ROOT);
                final Optional<String> field = seriesValue.series().field();

                final String fieldName;
                if (field.isPresent()) {
                    fieldName = String.format(Locale.ROOT, "aggregation_value_%s_%s", function, field.get());
                } else {
                    fieldName = String.format(Locale.ROOT, "aggregation_value_%s", function);
                }

                fields.put(fieldName, seriesValue.value());
            }

            // This is the concatenated key value
            fields.put("aggregation_key", keyString);

            // TODO: Can we find a useful source value?
            final Message message = new Message(eventMessage, "", result.effectiveTimerange().to());
            message.addFields(fields);

            LOG.debug("Creating event {}/{} - {} {} ({})", eventDefinition.title(), eventDefinition.id(), keyResult.key(), seriesString(keyResult), fields);
            eventsWithContext.add(EventWithContext.create(event, message));
        }

        return eventsWithContext.build();
    }

    // Build a human readable event message string that contains somewhat useful information
    private String createEventMessageString(String keyString, AggregationKeyResult keyResult) {
        final StringBuilder builder = new StringBuilder(eventDefinition.title()).append(": ");

        if (!keyResult.key().isEmpty()) {
            builder.append(keyString).append(" - ");
        }

        for (AggregationSeriesValue seriesValue : keyResult.seriesValues()) {
            final AggregationSeries series = seriesValue.series();
            final String functionName = series.function().toString().toLowerCase(Locale.ROOT);
            final String functionField = series.field().orElse("");

            builder.append(functionName).append("(").append(functionField).append(")");
            builder.append("=").append(seriesValue.value());
            builder.append(" ");
        }

        return builder.toString().trim();
    }

    // Only used to create log messages
    private String seriesString(AggregationKeyResult keyResult) {
        return keyResult.seriesValues().stream()
                .map(seriesValue -> String.format(Locale.ROOT, "%s(%s)=%s", seriesValue.series().function().toString().toLowerCase(Locale.ROOT), seriesValue.series().field().orElse(""), seriesValue.value()))
                .collect(Collectors.joining(" "));
    }
}
