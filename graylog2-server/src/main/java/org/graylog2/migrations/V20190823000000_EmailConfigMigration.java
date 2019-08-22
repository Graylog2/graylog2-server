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
package org.graylog2.migrations;

import org.graylog2.email.configuration.EmailConfiguration;
import org.graylog2.email.configuration.EmailConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;

public class V20190823000000_EmailConfigMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20190823000000_EmailConfigMigration.class);

    private org.graylog2.configuration.EmailConfiguration oldEmailConfiguration;
    private EmailConfigurationService emailConfigurationService;

    @Inject
    public V20190823000000_EmailConfigMigration(org.graylog2.configuration.EmailConfiguration oldEmailConfiguration,
                                                EmailConfigurationService emailConfigurationService) {
        this.oldEmailConfiguration = oldEmailConfiguration;
        this.emailConfigurationService = emailConfigurationService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-08-23T00:00:00Z");
    }

    @Override
    public void upgrade() {
        final EmailConfiguration emailConfiguration = emailConfigurationService.load();
        if (emailConfiguration == null) {
            final EmailConfiguration toMigrateEmailConfiguration = EmailConfiguration.create(
                    oldEmailConfiguration.isEnabled(),
                    oldEmailConfiguration.getHostname(),
                    oldEmailConfiguration.getPort(),
                    oldEmailConfiguration.isUseAuth(),
                    oldEmailConfiguration.isUseTls(),
                    oldEmailConfiguration.isUseSsl(),
                    oldEmailConfiguration.getUsername(),
                    oldEmailConfiguration.getPassword(),
                    oldEmailConfiguration.getFromEmail(),
                    oldEmailConfiguration.getWebInterfaceUri()
            );
            emailConfigurationService.save(toMigrateEmailConfiguration);
            LOG.info("Migrated EmailConfiguration: {}", toMigrateEmailConfiguration);
        }
    }
}
