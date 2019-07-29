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
