/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.security.realm;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.util.CollectionUtils;
import org.graylog2.users.UserImpl;
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

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        final AuthenticationInfo authenticationInfo = super.doGetAuthenticationInfo(token);
        // After successful authentication, exchange the principals to unique admin userId
        if (authenticationInfo instanceof SimpleAccount) {
            SimpleAccount account = (SimpleAccount) authenticationInfo;
            account.setPrincipals(new SimplePrincipalCollection(UserImpl.LocalAdminUser.LOCAL_ADMIN_ID, NAME));
            return account;
        }
        return null;
    }
}
