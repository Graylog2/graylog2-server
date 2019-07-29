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
package org.graylog.events.notifications;

import com.google.common.collect.ImmutableList;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.processor.EventProcessor;
import org.graylog.events.processor.EventProcessorException;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.MessageSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

public class EventBacklogService {
    private static final Logger LOG = LoggerFactory.getLogger(EventBacklogService.class);

    private final Map<String, EventProcessor.Factory> eventProcessorFactories;
    private final DBEventDefinitionService eventDefinitionService;

    @Inject
    public EventBacklogService(Map<String, EventProcessor.Factory> eventProcessorFactories, DBEventDefinitionService eventDefinitionService) {
        this.eventProcessorFactories = eventProcessorFactories;
        this.eventDefinitionService = eventDefinitionService;
    }

    public ImmutableList<MessageSummary> getMessagesForEvent(EventDto eventDto, long backlogSize) throws NotFoundException {
        if (backlogSize <= 0) {
            return ImmutableList.of();
        }
        final EventProcessor.Factory factory = eventProcessorFactories.get(eventDto.eventDefinitionType());
        if (factory == null) {
            throw new NotFoundException("Couldn't find event processor factory for type " +
                    eventDto.eventDefinitionType());
        }
        final EventDefinition eventDefinition = eventDefinitionService.get(eventDto.eventDefinitionId()).orElseThrow(() ->
                new NotFoundException("Could not find event definintion <" +
                        eventDto.eventDefinitionId() + ">"));
        final EventProcessor eventProcessor = factory.create(eventDefinition);

        final ImmutableList.Builder<MessageSummary> backlogBuilder = ImmutableList.builder();
        try {
            eventProcessor.sourceMessagesForEvent(Event.fromDto(eventDto), backlogBuilder::addAll, backlogSize);
        } catch (EventProcessorException e) {
            // TODO return this error, so it can be included in the notification message?
            LOG.error("Failed to query backlog messages for Event {}", eventDto.id(), e);
        }
        return backlogBuilder.build();
    }
}
