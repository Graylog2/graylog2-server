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

import com.google.common.collect.ImmutableList;
import org.graylog.events.event.EventWithContext;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * The event handler engine is responsible for executing handlers on events.
 */
@Singleton
public class EventStorageHandlerEngine {
    private final Map<String, EventStorageHandler.Factory> storageHandlerFactories;

    @Inject
    public EventStorageHandlerEngine(Map<String, EventStorageHandler.Factory> storageHandlerFactories) {
        this.storageHandlerFactories = storageHandlerFactories;
    }

    /**
     * Run storage handlers. These persist events into a storage system and they must run after the notification
     * handler has run.
     *
     * @param eventsWithContext events to run the handlers on
     * @param handlerConfigs    event definition
     */
    public void handleEvents(List<EventWithContext> eventsWithContext, ImmutableList<EventStorageHandler.Config> handlerConfigs) throws EventStorageHandlerException {
        for (final EventStorageHandler.Config config : handlerConfigs) {
            final EventStorageHandler.Factory storageHandlerFactory = storageHandlerFactories.get(config.type());

            if (storageHandlerFactory == null) {
                throw new EventStorageHandlerException("Couldn't find storage handler for type <" + config.type() + ">", config);
            }

            try {
                final EventStorageHandler storageHandler = storageHandlerFactory.create(config);
                final EventStorageHandlerCheckResult checkResult = storageHandler.checkPreconditions();

                if (checkResult.canExecute()) {
                    storageHandler.handleEvents(eventsWithContext);
                } else {
                    throw new EventStorageHandlerException("Precondition for storage handler <" + config.type() + "> failed: " + checkResult.message(), config);
                }
            } catch (Exception e) {
                throw new EventStorageHandlerException("Couldn't execute storage handler <" + config.type() + ">", config, e);
            }
        }
    }
}
