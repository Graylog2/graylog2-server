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
package org.graylog2.shared.initializers;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager.Listener;
import org.graylog2.plugin.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ServiceManagerListener extends Listener {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManagerListener.class);
    private final ServerStatus serverStatus;

    @Inject
    public ServiceManagerListener(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    @Override
    public void healthy() {
        LOG.info("Services are healthy");
        serverStatus.start();
    }

    @Override
    public void stopped() {
        LOG.info("Services are now stopped.");
    }

    @Override
    public void failure(Service service) {
        // do not log the failure here again, the ServiceManager itself does so already on Level ERROR.
        serverStatus.fail();
    }
}
