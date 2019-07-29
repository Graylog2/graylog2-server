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
package org.graylog.events.event;

import de.huxhorn.sulky.ulid.ULID;
import org.graylog.events.processor.EventDefinition;
import org.graylog2.cluster.NodeNotFoundException;
import org.graylog2.cluster.NodeService;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;

public class EventProcessorEventFactory implements EventFactory {
    private final String source;
    private final ULID ulid;

    @Inject
    public EventProcessorEventFactory(ULID ulid, NodeService nodeService, NodeId nodeId) {
        this.ulid = ulid;
        try {
            // TODO: This can fail depending on when it's called. Check if we can load it in create() and cache it
            this.source = nodeService.byNodeId(nodeId).getHostname();
        } catch (NodeNotFoundException e) {
            throw new RuntimeException("Couldn't get node ID", e);
        }
    }

    @Override
    public Event createEvent(EventDefinition eventDefinition, DateTime eventTime, String message) {
        return new EventImpl(
                ulid.nextULID(),
                eventTime.withZone(DateTimeZone.UTC),
                eventDefinition.config().type(),
                eventDefinition.id(),
                message,
                source,
                eventDefinition.priority(),
                eventDefinition.alert()
        );
    }
}
