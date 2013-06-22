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

import org.graylog2.systemjobs.SystemJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class FixDeflectorByMoveJob extends SystemJob {

    private static final Logger LOG = LoggerFactory.getLogger(FixDeflectorByMoveJob.class);

    private boolean cancelRequested = false;

    @Override
    public void execute() {

        // wait until all buffered and cached messages are written
    }

    @Override
    public void requestCancel() {
        this.cancelRequested = true;
    }

    @Override
    public int getProgress() {
        return 0;
    }

    @Override
    public boolean providesProgress() {
        return false;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public String getDescription() {
        return "Tries to fix a broken deflector alias by converting the deflector index to a valid index. Triggered " +
                "by hand after a notification. This operation can take some time depending on the number of messages " +
                "that were already written into the deflector index.";
    }

}
