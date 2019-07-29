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
