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
import com.google.common.collect.Maps;
import org.graylog.events.conditions.BooleanNumberConditionsVisitor;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventFactory;
import org.graylog.events.event.EventReplayInfo;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.processor.EventConsumer;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.EventStreamService;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog.plugins.views.search.searchtypes.pivot.HasField;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.HasOptionalField;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AggregationSearchUtils {
    private final Logger LOG = LoggerFactory.getLogger(AggregationSearchUtils.class);

    private final EventDefinition eventDefinition;
    private final AggregationEventProcessorConfig config;
    private final Set<EventQuerySearchTypeSupplier> eventQueryModifiers;
    private final AggregationSearch.Factory aggregationSearchFactory;
    private final EventStreamService eventStreamService;
    private final MessageFactory messageFactory;
    private final PermittedStreams permittedStreams;

    public AggregationSearchUtils(EventDefinition eventDefinition,
                                  AggregationEventProcessorConfig config,
                                  Set<EventQuerySearchTypeSupplier> eventQueryModifiers,
                                  AggregationSearch.Factory aggregationSearchFactory,
                                  EventStreamService eventStreamService,
                                  MessageFactory messageFactory,
                                  PermittedStreams permittedStreams) {
        this.eventDefinition = eventDefinition;
        this.config = config;
        this.eventQueryModifiers = eventQueryModifiers;
        this.aggregationSearchFactory = aggregationSearchFactory;
        this.eventStreamService = eventStreamService;
        this.messageFactory = messageFactory;
        this.permittedStreams = permittedStreams;
    }

    public void aggregatedSearch(EventFactory eventFactory, AggregationEventProcessorParameters parameters,
                                 EventConsumer<List<EventWithContext>> eventsConsumer) throws EventProcessorException {
        aggregatedSearch(eventFactory, parameters, eventsConsumer, (event) -> null);
    }

    public void aggregatedSearch(EventFactory eventFactory, AggregationEventProcessorParameters parameters,
                                 EventConsumer<List<EventWithContext>> eventsConsumer, Function<Event, Void> eventDecorator) throws EventProcessorException {
        final var owner = new AggregationSearch.User("event-processor-" + AggregationEventProcessorConfig.TYPE_NAME + "-" + eventDefinition.id(), DateTimeZone.UTC);
        final List<SearchType> additionalSearchTypes = eventQueryModifiers.stream()
                .flatMap(e -> e.additionalSearchTypes(eventDefinition).stream())
                .toList();
        final AggregationSearch search = aggregationSearchFactory.create(config, parameters, owner, eventDefinition, additionalSearchTypes);
        final AggregationResult result = search.doSearch();

        if (result.keyResults().isEmpty()) {
            LOG.debug("Aggregated search returned empty result set.");
            return;
        }

        LOG.debug("Got {} (total-aggregated-messages={}) results.", result.keyResults().size(), result.totalAggregatedMessages());

        eventsConsumer.accept(eventsFromAggregationResult(eventFactory, parameters, result, eventDecorator));
    }

    @VisibleForTesting
    ImmutableList<EventWithContext> eventsFromAggregationResult(EventFactory eventFactory,
                                                                AggregationEventProcessorParameters parameters,
                                                                AggregationResult result,
                                                                Function<Event, Void> eventDecorator) throws EventProcessorException {
        final ImmutableList.Builder<EventWithContext> eventsWithContext = ImmutableList.builder();
        final Set<String> sourceStreams = eventStreamService.buildEventSourceStreams(getStreams(parameters),
                result.sourceStreams());

        for (final AggregationKeyResult keyResult : result.keyResults()) {
            if (!satisfiesConditions(keyResult)) {
                LOG.debug("Skipping result <{}> because the conditions <{}> don't match", keyResult, config.conditions());
                continue;
            }

            final String keyString = String.join("|", keyResult.key());
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
                    .filters(config.filters())
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
                final String function = seriesValue.series().type().toLowerCase(Locale.ROOT);
                final Optional<String> field = fieldFromSeries(seriesValue.series());

                final String fieldName = field.map(f -> String.format(Locale.ROOT, "aggregation_value_%s_%s", function, f))
                        .orElseGet(() -> String.format(Locale.ROOT, "aggregation_value_%s", function));

                fields.put(fieldName, seriesValue.value());
            }

            // This is the concatenated key value
            fields.put("aggregation_key", keyString);

            // TODO: Can we find a useful source value?
            final Message message = messageFactory.createMessage(eventMessage, "", result.effectiveTimerange().to());
            message.addFields(fields);

            // Ask any event query modifier for its state and collect it into the event modifier state
            final Map<String, Object> eventModifierState = eventQueryModifiers.stream()
                    .flatMap(modifier -> modifier.eventModifierData(result.additionalResults()).entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // Apply event processor specific decorator to event
            eventDecorator.apply(event);

            LOG.debug("Creating event {}/{} - {} {} ({})", eventDefinition.title(), eventDefinition.id(), keyResult.key(), seriesString(keyResult), fields);

            eventsWithContext.add(EventWithContext.builder()
                    .event(event)
                    .messageContext(message)
                    .eventModifierState(eventModifierState)
                    .build());
        }

        return eventsWithContext.build();
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

    private Optional<String> fieldFromSeries(SeriesSpec series) {
        if (series instanceof HasField hasField) {
            return Optional.ofNullable(hasField.field());
        }
        if (series instanceof HasOptionalField hasOptionalField) {
            return hasOptionalField.field();
        }

        return Optional.empty();
    }

    // Build a human-readable event message string that contains somewhat useful information
    private String createEventMessageString(String keyString, AggregationKeyResult keyResult) {
        final StringBuilder builder = new StringBuilder(eventDefinition.title()).append(": ");

        if (!keyResult.key().isEmpty()) {
            builder.append(keyString).append(" - ");
        }

        builder.append(seriesString(keyResult));

        return builder.toString().trim();
    }

    // Only used to create log messages
    private String seriesString(AggregationKeyResult keyResult) {
        return keyResult.seriesValues().stream()
                .map(this::formatSeriesValue)
                .collect(Collectors.joining(" "));
    }

    private String formatSeriesValue(AggregationSeriesValue seriesValue) {
        return String.format(Locale.ROOT, "%s=%s", seriesValue.series().literal(), seriesValue.value());
    }
}
