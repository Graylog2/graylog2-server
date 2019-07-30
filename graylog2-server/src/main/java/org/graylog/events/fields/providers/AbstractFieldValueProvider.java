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
package org.graylog.events.fields.providers;

import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFieldValueProvider implements FieldValueProvider {
    public interface Factory<TYPE extends FieldValueProvider> extends FieldValueProvider.Factory<TYPE> {
        @Override
        TYPE create(Config config);
    }

    private static final Logger LOG = LoggerFactory.getLogger(AbstractFieldValueProvider.class);

    private final Config config;

    public AbstractFieldValueProvider(Config config) {
        this.config = config;
    }

    @Override
    public FieldValue get(String fieldName, EventWithContext eventWithContext) {
        try {
            return doGet(fieldName, eventWithContext);
        } catch (Exception e) {
            LOG.error("Couldn't execute field value provider: {}", config, e);
            return FieldValue.error();
        }
    }

    protected abstract FieldValue doGet(String fieldName, EventWithContext eventWithContext);
}
