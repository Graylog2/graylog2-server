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
package org.graylog2.caches;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import org.graylog2.inputs.Cache;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Caches {

    private static final Logger LOG = LoggerFactory.getLogger(Caches.class);
    private final Cache inputCache;
    private final Cache outputCache;

    private final int MAXTRIES = 30;

    @Inject
    public Caches(InputCache inputCache, OutputCache outputCache) {
        this.inputCache = inputCache;
        this.outputCache = outputCache;
    }

    public void waitForEmptyCaches() {
        // Wait until the buffers are empty. Messages that were already started to be processed must be fully processed.
        LOG.info("Waiting until all caches are empty.");
        int tries = 0;
        while(!(inputCache.isEmpty() && outputCache.isEmpty())) {
            tries++;
            if (tries >= MAXTRIES) {
                LOG.info("Waited for {} seconds, giving up.", tries);
                return;
            }
            LOG.info("Not all caches are empty. Waiting another second. ({}imc/{}omc)", inputCache.size(), outputCache.size());
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
        }

        LOG.info("All caches are empty. Continuing.");
    }

}
