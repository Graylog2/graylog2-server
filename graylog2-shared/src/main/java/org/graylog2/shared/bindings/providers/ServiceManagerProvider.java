/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.bindings.providers;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Set;

public class ServiceManagerProvider implements Provider<ServiceManager> {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManagerProvider.class);

    @Inject
    Set<Service> services = Sets.<Service>newHashSet(new AbstractService() {
        @Override
        protected void doStart() {
        }

        @Override
        protected void doStop() {

        }
    });

    @Override
    public ServiceManager get() {
        LOG.debug("Using services: {}", services);
        return new ServiceManager(services);
    }
}
