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
package org.graylog2.system.jobs;

import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;

import javax.inject.Inject;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobFactory {
    private final FixDeflectorByMoveJob.Factory fixDeflectorByMoveJobFactory;
    private final FixDeflectorByDeleteJob.Factory fixDeflectorByDeleteJobFactory;

    @Inject
    public SystemJobFactory(FixDeflectorByMoveJob.Factory fixDeflectorByMoveJobFactory,
                            FixDeflectorByDeleteJob.Factory fixDeflectorByDeleteJobFactory) {
        this.fixDeflectorByMoveJobFactory = fixDeflectorByMoveJobFactory;
        this.fixDeflectorByDeleteJobFactory = fixDeflectorByDeleteJobFactory;
    }

    public SystemJob build(String jobName) throws NoSuchJobException {
        switch(SystemJob.Type.valueOf(jobName.toUpperCase())) {
            case FIX_DEFLECTOR_DELETE_INDEX:
                return fixDeflectorByDeleteJobFactory.create();
            case FIX_DEFLECTOR_MOVE_INDEX:
                return fixDeflectorByMoveJobFactory.create();
        }

        throw new NoSuchJobException();
    }

}
