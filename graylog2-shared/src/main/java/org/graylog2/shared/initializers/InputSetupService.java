/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.shared.initializers;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputSetupService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(InputSetupService.class);
    private final InputRegistry inputRegistry;

    @Inject
    public InputSetupService(InputRegistry inputRegistry) {
        this.inputRegistry = inputRegistry;
    }

    @Override
    protected void startUp() throws Exception {
        inputRegistry.launchAllPersisted();
    }

    @Override
    protected void shutDown() throws Exception {
        for (InputState state : inputRegistry.getRunningInputs()) {
            MessageInput input = state.getMessageInput();

            LOG.info("Attempting to close input <{}> [{}].", input.getUniqueReadableId(), input.getName());

            Stopwatch s = Stopwatch.createStarted();
            try {
                input.stop();

                LOG.info("Input <{}> closed. Took [{}ms]", input.getUniqueReadableId(), s.elapsed(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                LOG.error("Unable to stop input <{}> [{}]: " + e.getMessage(), input.getUniqueReadableId(), input.getName());
            } finally {
                s.stop();
            }
        }
    }
}
