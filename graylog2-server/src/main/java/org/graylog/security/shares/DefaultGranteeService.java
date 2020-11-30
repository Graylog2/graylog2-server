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
package org.graylog.security.shares;

import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.GranteeAuthorizer;
import org.graylog.security.shares.EntityShareResponse.AvailableGrantee;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class DefaultGranteeService implements GranteeService {
    protected final UserService userService;
    protected final GRNRegistry grnRegistry;
    protected final GranteeAuthorizer.Factory granteeAuthorizerFactory;

    @Inject
    public DefaultGranteeService(UserService userService, GRNRegistry grnRegistry, GranteeAuthorizer.Factory granteeAuthorizerFactory) {
        this.userService = userService;
        this.grnRegistry = grnRegistry;
        this.granteeAuthorizerFactory = granteeAuthorizerFactory;
    }

    @Override
    public ImmutableSet<AvailableGrantee> getAvailableGrantees(User sharingUser) {
        return ImmutableSet.<AvailableGrantee>builder()
                .addAll(getAvailableUserGrantees(sharingUser))
                .add(getGlobalGrantee())
                .build();
    }

    @Override
    public Set<GRN> getGranteeAliases(GRN grantee) {
        return Collections.singleton(grantee);
    }

    @Override
    public Set<User> getVisibleUsers(User requestingUser) {
        final GranteeAuthorizer userAuthorizer = granteeAuthorizerFactory.create(requestingUser);

        if (userAuthorizer.isPermitted(RestPermissions.USERS_LIST)) {
            return userService.loadAll().stream().collect(ImmutableSet.toImmutableSet());
        } else {
            return userService.loadAll().stream()
                    .filter(u -> userAuthorizer.isPermitted(RestPermissions.USERS_READ, u.getName()))
                    .collect(ImmutableSet.toImmutableSet());
        }
    }

    private ImmutableSet<AvailableGrantee> getAvailableUserGrantees(User sharingUser) {
        return getVisibleUsers(sharingUser).stream()
                // Don't return the sharing user in available grantees until we want to support that sharing users
                // can remove themselves from an entity.
                .filter(user -> !sharingUser.getId().equals(user.getId()))
                .map(user -> AvailableGrantee.create(
                        grnRegistry.ofUser(user),
                        "user",
                        user.getFullName()
                ))
                .collect(ImmutableSet.toImmutableSet());
    }

    private AvailableGrantee getGlobalGrantee() {
        return AvailableGrantee.create(
                GRNRegistry.GLOBAL_USER_GRN,
                "global",
                "Everyone"
        );
    }

}
