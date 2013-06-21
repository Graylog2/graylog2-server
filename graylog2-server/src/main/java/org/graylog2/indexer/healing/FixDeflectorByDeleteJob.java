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

package org.graylog2.indexer.healing;

import org.graylog2.Core;
import org.graylog2.systemjobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FixDeflectorByDeleteJob extends SystemJob {

    private static final Logger LOG = LoggerFactory.getLogger(FixDeflectorByDeleteJob.class);

    private final Core core;

    private boolean cancelRequested = false;
    private int progress = 0;

    public FixDeflectorByDeleteJob(Core core) {
        this.core = core;
    }

    @Override
    public void execute() {
        /*
        // Pause message processing.
        core.pauseMessageProcessing();

        // Clear all buffers and caches. They would be written to the deflector index anyway and this is the delete strategy.

        progress = 25;

        // Delete deflector index.
        progress = 50;

        // Set up deflector.
        progress = 75;

        // Start message processing again.
        core.restartMessageProcessing();
        progress = 100;
        */
    }

    @Override
    public void requestCancel() {
        this.cancelRequested = true;
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public boolean providesProgress() {
        return true;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Tries to fix a broken deflector alias by deleting the deflector index. Triggered by hand " +
                "after a notification.";
    }

}
