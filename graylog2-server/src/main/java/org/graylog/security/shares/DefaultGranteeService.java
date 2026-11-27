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
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.GranteeAuthorizer;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.UserAndTeamsConfig;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                .filter(grantee -> isAllowedType(config, grantee) ||
                        (activeShares != null && activeShares.stream()
                                .anyMatch(activeShare -> activeShare.grantee().equals(grantee.grn()))))
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

    @Override
    public Set<Grantee> getVisibleGrantees(User viewer, Set<GRN> candidates) {
        return getVisibleGrantees(viewer, candidates, Set.of());
    }

    /**
     * Filters the user and "Everyone" grantees in the given candidates. Other grantee types are dropped.
     *
     * @param viewer                    the user who looks at the grantees
     * @param candidates                the grantee GRNs to filter
     * @param additionalVisibleUserIds  IDs of users that are visible to the viewer regardless of user permissions
     * @return the visible user and "Everyone" grantees
     */
    protected ImmutableSet<Grantee> getVisibleGrantees(User viewer, Set<GRN> candidates, Set<String> additionalVisibleUserIds) {
        final ImmutableSet.Builder<Grantee> builder = ImmutableSet.<Grantee>builder()
                .addAll(getVisibleUserGrantees(viewer, candidates, additionalVisibleUserIds));

        if (candidates.contains(GRNRegistry.GLOBAL_USER_GRN)) {
            getGlobalGrantee().ifPresent(builder::add);
        }

        return builder.build();
    }

    private ImmutableSet<Grantee> getVisibleUserGrantees(User viewer, Set<GRN> candidates, Set<String> additionalVisibleUserIds) {
        // The share dialog only offers users stored in the database. That excludes the local admin user, which
        // has no valid ObjectId.
        final Set<String> userIds = candidates.stream()
                .filter(grn -> GRNTypes.USER.equals(grn.grnType()))
                .map(GRN::entity)
                .filter(ObjectId::isValid)
                .filter(id -> !id.equals(viewer.getId()))
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return ImmutableSet.of();
        }

        final GranteeAuthorizer userAuthorizer = granteeAuthorizerFactory.create(viewer);
        final boolean canListUsers = userAuthorizer.isPermitted(RestPermissions.USERS_LIST);

        return userService.loadByIds(userIds).stream()
                .filter(user -> canListUsers
                        || additionalVisibleUserIds.contains(user.getId())
                        || userAuthorizer.isPermitted(RestPermissions.USERS_READ, user.getName()))
                .map(user -> Grantee.createUser(grnRegistry.ofUser(user), user.getFullName()))
                .collect(ImmutableSet.toImmutableSet());
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
