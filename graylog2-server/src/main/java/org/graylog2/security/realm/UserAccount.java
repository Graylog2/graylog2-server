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
package org.graylog2.security.realm;

import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.graylog2.plugin.database.users.User;

import java.util.Collection;
import java.util.Set;

public class UserAccount extends SimpleAccount {
    private final User user;

    public UserAccount(User user) {
        this.user = user;
    }

    public UserAccount(Object principal, Object credentials, String realmName, User user) {
        super(principal, credentials, realmName);
        this.user = user;
    }

    public UserAccount(Object principal, Object hashedCredentials, ByteSource credentialsSalt, String realmName, User user) {
        super(principal, hashedCredentials, credentialsSalt, realmName);
        this.user = user;
    }

    public UserAccount(Collection principals, Object credentials, String realmName, User user) {
        super(principals, credentials, realmName);
        this.user = user;
    }

    public UserAccount(PrincipalCollection principals, Object credentials, User user) {
        super(principals, credentials);
        this.user = user;
    }

    public UserAccount(PrincipalCollection principals, Object hashedCredentials, ByteSource credentialsSalt, User user) {
        super(principals, hashedCredentials, credentialsSalt);
        this.user = user;
    }

    public UserAccount(PrincipalCollection principals, Object credentials, Set<String> roles, User user) {
        super(principals, credentials, roles);
        this.user = user;
    }

    public UserAccount(Object principal, Object credentials, String realmName, Set<String> roleNames, Set<Permission> permissions, User user) {
        super(principal, credentials, realmName, roleNames, permissions);
        this.user = user;
    }

    public UserAccount(Collection principals, Object credentials, String realmName, Set<String> roleNames, Set<Permission> permissions, User user) {
        super(principals, credentials, realmName, roleNames, permissions);
        this.user = user;
    }

    public UserAccount(PrincipalCollection principals, Object credentials, Set<String> roleNames, Set<Permission> permissions, User user) {
        super(principals, credentials, roleNames, permissions);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
