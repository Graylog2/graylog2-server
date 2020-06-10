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
package org.graylog2.periodical;

import org.graylog2.cluster.LdapGroupMappingMigrationState;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Converts pre-2.0 ldap group mappings to use a list of maps but leaves the original key in the collection to make it possible to downgrade.
 * Pre 2.0 format: group_role_mapping -> { "groupname" => "role_id" }
 * new format: group_role_mapping_list -> [ {group => "groupname", role_id => "roleid" }]
 *
 * This makes it possible to use '.' in the group names
 */
public class LdapGroupMappingMigration extends Periodical {
    private static final Logger log = LoggerFactory.getLogger(LdapGroupMappingMigration.class);
    private final ClusterConfigService clusterConfigService;
    private final LdapSettingsService ldapSettingsService;

    @Inject
    public LdapGroupMappingMigration(ClusterConfigService clusterConfigService, LdapSettingsService ldapSettingsService) {
        this.clusterConfigService = clusterConfigService;
        this.ldapSettingsService = ldapSettingsService;
    }

    @Override
    public void doRun() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings != null) {
            ldapSettings.setGroupMapping(ldapSettings.getGroupMapping());
            try {
                ldapSettingsService.save(ldapSettings);
                clusterConfigService.write(LdapGroupMappingMigrationState.create(true));
                log.info("Migrated LDAP group mapping format");
            } catch (ValidationException e) {
                log.error("Unable to save migrated LDAP settings!", e);
            }
        }
    }

    @Override
    public boolean startOnThisNode() {
        final LdapGroupMappingMigrationState migrationState =
                clusterConfigService.getOrDefault(LdapGroupMappingMigrationState.class,
                                                  LdapGroupMappingMigrationState.create(false));
        return !migrationState.migrationDone();
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean primaryOnly() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
