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
package org.graylog.events;

import org.graylog.events.configuration.EventsConfiguration;
import org.graylog.events.configuration.EventsConfigurationProvider;

public class EventsConfigurationTestProvider extends EventsConfigurationProvider {
    private final EventsConfiguration config;

    public static EventsConfigurationTestProvider create() {
        return new EventsConfigurationTestProvider(EventsConfiguration.builder().build());
    }

    public EventsConfigurationTestProvider(EventsConfiguration config) {
        super(null);
        this.config = config;
    }

    @Override
    public EventsConfiguration get() {
        return config;
    }
}
