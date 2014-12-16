/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.radio.inputs;

import com.google.common.eventbus.Subscribe;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.cluster.InputService;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class InputStateListener {
    private static final Logger LOG = LoggerFactory.getLogger(InputStateListener.class);
    private final InputService inputService;
    private final InputRegistry inputRegistry;

    @Inject
    public InputStateListener(InputService inputService, InputRegistry inputRegistry) {
        this.inputService = inputService;
        this.inputRegistry = inputRegistry;
    }

    @Subscribe public void inputStateChanged(IOState<MessageInput> state) {
        MessageInput input = state.getStoppable();
        try {
            if (!input.getGlobal())
                inputService.unregisterInCluster(input);
        } catch (Exception e) {
            LOG.error("Could not unregister input [{}], id <{}> on server cluster: {}", input.getName(), input.getId(), e);
            return;
        }

        LOG.info("Unregistered input [{}], id <{}> on server cluster.", input.getName(), input.getId());

        inputRegistry.removeFromRunning(state);
    }
}
