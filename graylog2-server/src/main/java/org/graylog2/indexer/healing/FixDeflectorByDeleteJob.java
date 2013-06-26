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
import org.graylog2.ProcessingPauseLockedException;
import org.graylog2.indexer.Deflector;
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
        LOG.info("Attempting to fix deflector with delete strategy.");

        // Pause message processing and lock the pause.
        core.pauseMessageProcessing(true);

        // Clear all caches. They would be written to the deflector index anyway and this is the delete strategy.
        core.getInputCache().clear();
        core.getOutputCache().clear();

        // Wait until the buffers are empty. Messages that where already started to be processed must be fully processed.
        while(true) {
            if(core.getProcessBuffer().isEmpty() && core.getOutputBuffer().isEmpty()) {
                break;
            }

            try {
                LOG.info("Not all buffers are empty. Waiting another second.");
                Thread.sleep(1000);
            } catch (InterruptedException e) { /* */ }
        }

        LOG.info("All buffers and caches are empty. Continuing.");

        progress = 25;

        // Delete deflector index.
        LOG.info("Deleting <{}> index.", Deflector.DEFLECTOR_NAME);
        core.getIndexer().deleteIndex(Deflector.DEFLECTOR_NAME);
        progress = 50;

        // Set up deflector.
        progress = 75;

        // Start message processing again.
        try {
            core.unlockProcessingPause();
            core.resumeMessageProcessing();
        } catch (ProcessingPauseLockedException e) {
            // lol checked exceptions
            throw new RuntimeException("Could not unlock processing pause.", e);
        }

        progress = 100;
        LOG.info("Finished.");
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
