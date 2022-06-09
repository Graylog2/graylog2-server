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
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserAndTeamsConfig;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class DefaultGranteeService implements GranteeService {
    protected final UserService userService;
    protected final GRNRegistry grnRegistry;
    protected final GranteeAuthorizer.Factory granteeAuthorizerFactory;
    protected final ClusterConfigService clusterConfigService;

    @Inject
    public DefaultGranteeService(UserService userService,
                                 GRNRegistry grnRegistry,
                                 GranteeAuthorizer.Factory granteeAuthorizerFactory,
                                 ClusterConfigService clusterConfigService) {
        this.userService = userService;
        this.grnRegistry = grnRegistry;
        this.granteeAuthorizerFactory = granteeAuthorizerFactory;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public ImmutableSet<Grantee> getAvailableGrantees(User sharingUser) {
        final ImmutableSet.Builder<Grantee> builder = ImmutableSet.<Grantee>builder()
                .addAll(getAvailableUserGrantees(sharingUser));

        getGlobalGrantee().ifPresent(builder::add);

        return builder.build();
    }

    @Override
    public ImmutableSet<Grantee> getModifiableGrantees(Set<Grantee> availableGrantees, ImmutableSet<EntityShareResponse.ActiveShare> activeShares) {
        final UserAndTeamsConfig config = clusterConfigService.getOrDefault(UserAndTeamsConfig.class, UserAndTeamsConfig.DEFAULT_VALUES);
        return availableGrantees.stream()
                .filter(grantee -> isAllowedType(config, grantee) || activeShares.stream().
                        anyMatch(activeShare -> activeShare.grantee().equals(grantee.grn())))
                .collect(ImmutableSet.toImmutableSet());
    }

    protected boolean isAllowedType(UserAndTeamsConfig config, Grantee grantee) {
        final boolean permittedGlobal = config.sharingWithEveryone() && Grantee.GRANTEE_TYPE_GLOBAL.equals(grantee.type());
        final boolean permittedUser = config.sharingWithUsers() && Grantee.GRANTEE_TYPE_USER.equals(grantee.type());
        return permittedGlobal || permittedUser;
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

    private ImmutableSet<Grantee> getAvailableUserGrantees(User sharingUser) {
        return getVisibleUsers(sharingUser).stream()
                // Don't return the sharing user in available grantees until we want to support that sharing users
                // can remove themselves from an entity.
                .filter(user -> !sharingUser.getId().equals(user.getId()))
                .map(user -> Grantee.createUser(
                        grnRegistry.ofUser(user),
                        user.getFullName()
                ))
                .collect(ImmutableSet.toImmutableSet());
    }

    private Optional<Grantee> getGlobalGrantee() {
        return Optional.of(Grantee.createGlobal());
    }
}
