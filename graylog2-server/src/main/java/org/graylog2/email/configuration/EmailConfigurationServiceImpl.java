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
package org.graylog2.email.configuration;

import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.inject.Inject;

public class EmailConfigurationServiceImpl implements EmailConfigurationService {

    private ClusterConfigService clusterConfigService;

    @Inject
    public EmailConfigurationServiceImpl(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }
    @Override
    public void save(EmailConfiguration emailConfiguration) {
        clusterConfigService.write(emailConfiguration);
    }

    @Override
    public EmailConfiguration load() {
        return clusterConfigService.get(EmailConfiguration.class);
    }
}
