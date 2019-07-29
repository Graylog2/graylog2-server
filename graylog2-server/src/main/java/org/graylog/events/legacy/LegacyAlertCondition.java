package org.graylog.events.legacy;

import com.google.common.collect.ImmutableMap;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventDefinition;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;

/**
 * This is used to support legacy {@link org.graylog2.plugin.alarms.callbacks.AlarmCallback}s. An alarm callback
 * expects an instance of an {@link AlertCondition}. This is basically just a small wrapper around
 * {@link AbstractAlertCondition} to act as a dummy.
 */
public class LegacyAlertCondition extends AbstractAlertCondition {
    private final String description;

    LegacyAlertCondition(Stream stream,
                         EventDefinition eventDefinition,
                         EventDto eventDto) {
        super(
                stream,
                eventDefinition.id(),
                eventDefinition.config().type(),
                eventDto.processingTimestamp(),
                "admin",
                ImmutableMap.of("backlog", 50), // TODO: Use value from notification config once it's configurable
                eventDefinition.title()
        );
        this.description = eventDefinition.title();
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public AlertCondition.CheckResult runCheck() {
        throw new UnsupportedOperationException("Running LegacyAlertCondition is not supported!");
    }
}
