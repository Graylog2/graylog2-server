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
package org.graylog2.bindings.providers;

import javax.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.security.ldap.LdapConnector;

import javax.inject.Provider;


/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class LdapConnectorProvider implements Provider<LdapConnector> {
    private static LdapConnector ldapConnector = null;

    @Inject
    public LdapConnectorProvider(final Configuration configuration) {
        if (ldapConnector == null) {
            ldapConnector = new LdapConnector(configuration.getLdapConnectionTimeout());
        }
    }

    @Override
    public LdapConnector get() {
        return ldapConnector;
    }
}
