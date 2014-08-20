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
package org.graylog2.bindings.providers;

import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.system.jobs.SystemJobFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SystemJobFactoryProvider implements Provider<SystemJobFactory> {
    private static SystemJobFactory systemJobFactory = null;

    @Inject
    public SystemJobFactoryProvider(FixDeflectorByDeleteJob.Factory deleteJobFactory,
                                    FixDeflectorByMoveJob.Factory moveJobFactory) {
        if (systemJobFactory == null)
            systemJobFactory = new SystemJobFactory(moveJobFactory, deleteJobFactory);
    }

    @Override
    public SystemJobFactory get() {
        return systemJobFactory;
    }
}
