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
package org.graylog.events.fields;

import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.providers.FieldValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class EventFieldSpecEngine {
    private static final Logger LOG = LoggerFactory.getLogger(EventFieldSpecEngine.class);

    private final Map<String, FieldValueProvider.Factory> fieldValueProviders;

    @Inject
    public EventFieldSpecEngine(Map<String, FieldValueProvider.Factory> fieldValueProviders) {
        this.fieldValueProviders = fieldValueProviders;
    }

    public void execute(List<EventWithContext> eventsWithContext, Map<String, EventFieldSpec> fieldSpec) {
        for (final Map.Entry<String, EventFieldSpec> entry : fieldSpec.entrySet()) {
            final String fieldName = entry.getKey();
            final EventFieldSpec spec = entry.getValue();

            for (final FieldValueProvider.Config providerConfig : spec.providers()) {
                final FieldValueProvider.Factory providerFactory = fieldValueProviders.get(providerConfig.type());

                if (providerFactory == null) {
                    LOG.error("Couldn't find field provider factory for type {}", providerConfig.type());
                    continue;
                }

                final FieldValueProvider provider = providerFactory.create(providerConfig);

                for (final EventWithContext eventWithContext : eventsWithContext) {
                    final Event event = eventWithContext.event();
                    event.setField(fieldName, provider.get(fieldName, eventWithContext));
                }
            }
        }
    }
}
