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
package org.graylog.events.processor.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.event.EventWithContext;

import java.util.List;

public interface EventStorageHandler {
    interface Factory<TYPE extends EventStorageHandler> {
        TYPE create(Config config);
    }

    void handleEvents(List<EventWithContext> eventsWithContext);

    EventStorageHandlerCheckResult checkPreconditions();

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = Config.TYPE_FIELD,
            visible = true,
            defaultImpl = Config.FallbackEventStorageHandlerConfig.class)
    interface Config {
        String TYPE_FIELD = "type";

        @JsonProperty(TYPE_FIELD)
        String type();

        interface Builder<SELF> {
            @JsonProperty(TYPE_FIELD)
            SELF type(String type);
        }

        class FallbackEventStorageHandlerConfig implements Config {
            @Override
            public String type() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
