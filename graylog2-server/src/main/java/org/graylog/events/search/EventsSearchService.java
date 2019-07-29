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
package org.graylog.events.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesConfig;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.joda.time.DateTimeZone.UTC;

public class EventsSearchService {
    private static final Logger LOG = LoggerFactory.getLogger(EventsSearchService.class);

    // TODO: This needs the system events stream once it exists
    private static final Set<String> STREAM_FILTER = Stream.of(org.graylog2.plugin.streams.Stream.DEFAULT_EVENTS_STREAM_ID)
            .map(id -> String.join(":", Message.FIELD_STREAMS, id))
            .collect(Collectors.toSet());

    private final Searches searches;
    private final StreamService streamService;
    private final DBEventDefinitionService eventDefinitionService;
    private final ObjectMapper objectMapper;

    @Inject
    public EventsSearchService(Searches searches,
                               StreamService streamService,
                               DBEventDefinitionService eventDefinitionService,
                               ObjectMapper objectMapper) {
        this.searches = searches;
        this.streamService = streamService;
        this.eventDefinitionService = eventDefinitionService;
        this.objectMapper = objectMapper;
    }

    private String eventDefinitionFilter(String id) {
        return String.format(Locale.ROOT, "%s:%s", EventDto.FIELD_EVENT_DEFINITION_ID, id);
    }

    public EventsSearchResult search(EventsSearchParameters parameters) {
        final Sorting.Direction sortDirection = parameters.sortDirection() == EventsSearchParameters.SortDirection.ASC ? Sorting.Direction.ASC : Sorting.Direction.DESC;
        final ImmutableSet.Builder<String> filterBuilder = ImmutableSet.builder();

        filterBuilder.addAll(STREAM_FILTER);

        if (!parameters.filter().eventDefinitions().isEmpty()) {
            final String eventDefinitionFilter = parameters.filter().eventDefinitions().stream()
                    .map(this::eventDefinitionFilter)
                    .collect(Collectors.joining(" OR "));

            filterBuilder.addAll(Collections.singleton("(" + eventDefinitionFilter + ")"));
        }

        switch (parameters.filter().alerts()) {
            case INCLUDE:
                // Nothing to do
                break;
            case EXCLUDE:
                filterBuilder.add("NOT alert:true");
                break;
            case ONLY:
                filterBuilder.add("alert:true");
                break;
        }

        final SearchResult result = searches.search(SearchesConfig.builder()
                .query(parameters.query())
                .range(parameters.timerange())
                .filter(String.join(" AND ", filterBuilder.build()))
                .sorting(new Sorting(parameters.sortBy(), sortDirection))
                .offset((parameters.page() - 1) * parameters.perPage())
                .limit(parameters.perPage())
                .build());

        if (LOG.isDebugEnabled()) {
            try {
                LOG.debug("Search query\n{}", result.getBuiltQuery());
                LOG.debug("Search result\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            } catch (JsonProcessingException e) {
                LOG.error("Couldn't serialize search result", e);
            }
        }

        final ImmutableSet.Builder<String> eventDefinitionIdsBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> streamIdsBuilder = ImmutableSet.builder();

        final List<EventsSearchResult.Event> events = result.getResults().stream()
                .map(resultMsg -> {
                    final Message message = resultMsg.getMessage();

                    // HACK fix timestamp format because DateTime cannot parse our Elasticsearch date format
                    // TODO: Fix the timestamp issue in a nicer way. (e.g. custom deserializer that also works with mongojack)
                    message.addField(EventDto.FIELD_PROCESSING_TIMESTAMP, DateTime.parse(String.valueOf(message.getField(EventDto.FIELD_PROCESSING_TIMESTAMP)), Tools.timeFormatterWithOptionalMilliseconds().withZone(UTC)));
                    if (message.getField(EventDto.FIELD_TIMERANGE_START) != null) {
                        message.addField(EventDto.FIELD_TIMERANGE_START, DateTime.parse(String.valueOf(message.getField(EventDto.FIELD_TIMERANGE_START)), Tools.timeFormatterWithOptionalMilliseconds().withZone(UTC)));
                    }
                    if (message.getField(EventDto.FIELD_TIMERANGE_END) != null) {
                        message.addField(EventDto.FIELD_TIMERANGE_END, DateTime.parse(String.valueOf(message.getField(EventDto.FIELD_TIMERANGE_END)), Tools.timeFormatterWithOptionalMilliseconds().withZone(UTC)));
                    }

                    // Remove the _id field that has been added by our search code
                    final Map<String, Object> event = Maps.filterEntries(resultMsg.getMessage().getFields(), input -> !"_id".equals(input.getKey()));
                    final EventDto eventDto = objectMapper.convertValue(event, EventDto.class);

                    eventDefinitionIdsBuilder.add((String) resultMsg.getMessage().getField(EventDto.FIELD_EVENT_DEFINITION_ID));
                    streamIdsBuilder.addAll(resultMsg.getMessage().getStreamIds());

                    return EventsSearchResult.Event.create(eventDto, resultMsg.getIndex(), IndexMapping.TYPE_MESSAGE);
                }).collect(Collectors.toList());

        final EventsSearchResult.Context context = EventsSearchResult.Context.create(
                lookupEventDefinitions(eventDefinitionIdsBuilder.build()),
                lookupStreams(streamIdsBuilder.build())
        );

        return EventsSearchResult.builder()
                .parameters(parameters)
                .totalEvents(result.getTotalResults())
                .duration(result.tookMs())
                .events(events)
                .usedIndices(result.getUsedIndices().stream().map(IndexRange::indexName).collect(Collectors.toSet()))
                .context(context)
                .build();
    }

    private Map<String, EventsSearchResult.ContextEntity> lookupStreams(Set<String> streams) {
        return streams.stream()
                .map(streamId -> {
                    try {
                        return streamService.load(streamId);
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Persisted::getId, s -> EventsSearchResult.ContextEntity.create(s.getId(), s.getTitle(), s.getDescription())));
    }

    private Map<String, EventsSearchResult.ContextEntity> lookupEventDefinitions(Set<String> eventDefinitions) {
        return eventDefinitions.stream()
                .map(eventDefinitionService::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(EventDefinitionDto::id, d -> EventsSearchResult.ContextEntity.create(d.id(), d.title(), d.description())));
    }
}
