/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.caches;

import org.graylog2.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Caches {

    private static final Logger LOG = LoggerFactory.getLogger(Caches.class);

    public static void waitForEmptyCaches(Core core) {
        // Wait until the buffers are empty. Messages that were already started to be processed must be fully processed.
        while(true) {
            LOG.info("Waiting until all caches are empty.");
            if(core.getInputCache().size() == 0 && core.getOutputCache().size() == 0) {
                break;
            }

            try {
                LOG.info("Not all caches are empty. Waiting another second. ({}imc/{}omc)", core.getInputCache().size(), core.getOutputCache().size());
                Thread.sleep(1000);
            } catch (InterruptedException e) { /* */ }
        }

        LOG.info("All caches are empty. Continuing.");
    }

}
