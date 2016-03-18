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
package org.graylog2.shared.events;

import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadEventLoggingListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeadEventLoggingListener.class);

    @Subscribe
    public void handleDeadEvent(DeadEvent event) {
        LOGGER.warn("Received unhandled event of type <{}> from event bus <{}>", event.getEvent().getClass().getCanonicalName(),
                event.getSource().toString());
        LOGGER.debug("Dead event contents: {}", event.getEvent());
    }
}
