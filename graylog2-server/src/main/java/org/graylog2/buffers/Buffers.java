/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.buffers;

import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Buffers {

    private static final Logger LOG = LoggerFactory.getLogger(Buffers.class);

    public static void waitForEmptyBuffers(Core core) {
        // Wait until the buffers are empty. Messages that were already started to be processed must be fully processed.
        while(true) {
            LOG.info("Waiting until all buffers are empty.");
            if(core.getProcessBuffer().isEmpty() && core.getOutputBuffer().isEmpty()) {
                break;
            }

            try {
                LOG.info("Not all buffers are empty. Waiting another second. ({}p/{}o)", core.getProcessBuffer().getUsage(), core.getOutputBuffer().getUsage());
                Thread.sleep(1000);
            } catch (InterruptedException e) { /* */ }
        }

        LOG.info("All buffers are empty. Continuing.");
    }

}
