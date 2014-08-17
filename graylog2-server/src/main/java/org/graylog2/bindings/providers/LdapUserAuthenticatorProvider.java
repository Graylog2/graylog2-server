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

import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.users.UserService;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class LdapUserAuthenticatorProvider implements Provider<LdapUserAuthenticator> {
    private static LdapUserAuthenticator ldapUserAuthenticator = null;

    @Inject
    public LdapUserAuthenticatorProvider(LdapConnector ldapConnector,
                                         UserService userService,
                                         LdapSettingsService ldapSettingsService) {
        if (ldapUserAuthenticator == null)
            ldapUserAuthenticator = new LdapUserAuthenticator(ldapConnector, ldapSettingsService, userService);
    }

    @Override
    public LdapUserAuthenticator get() {
        return ldapUserAuthenticator;
    }
}
