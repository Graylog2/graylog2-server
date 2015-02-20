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
package org.graylog2.shared.inputs;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.events.inputs.IOStateChangedEvent;
import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InputStateListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputStateListener.class);

    @Inject
    public InputStateListener(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe public void inputStateChanged(IOStateChangedEvent<MessageInput> event) {
        final IOState<MessageInput> state = event.changedState();
        final MessageInput input = state.getStoppable();
        LOG.debug("Input State of {} changed: {} -> {}", input.getTitle(), event.oldState(), event.newState());
        final String msg = "Input [" + input.getName() + "/" + input.getId() + "] is now " + event.newState().toString();
        LOG.info(msg);
    }
}
