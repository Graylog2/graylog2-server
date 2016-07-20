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
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

public class RootAccountRealm extends SimpleAccountRealm {
    private static final Logger LOG = LoggerFactory.getLogger(RootAccountRealm.class);
    public static final String NAME = "root-user";

    @Inject
    RootAccountRealm(@Named("root_username") String rootUsername,
                     @Named("root_password_sha2") String rootPasswordSha2) {
        setCachingEnabled(false);
        setCredentialsMatcher(new HashedCredentialsMatcher("SHA-256"));
        setName("root-account-realm");

        addRootAccount(rootUsername, rootPasswordSha2);
    }

    private void addRootAccount(String username, String password) {
        LOG.debug("Adding root account named {}, having all permissions", username);
        add(new SimpleAccount(
                username,
                password,
                getName(),
                CollectionUtils.asSet("root"),
                CollectionUtils.<Permission>asSet(new AllPermission())
        ));
    }

}
