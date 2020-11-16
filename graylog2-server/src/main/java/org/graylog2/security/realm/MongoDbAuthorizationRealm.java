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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.PermissionAndRoleResolver;
import org.graylog2.plugin.database.users.User;
import org.graylog2.security.MongoDbAuthorizationCacheManager;
import org.graylog2.shared.security.ShiroRequestHeadersBinder;
import org.graylog2.shared.users.UserService;
import org.graylog2.users.events.UserChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.graylog2.shared.rest.RequestIdFilter.X_REQUEST_ID;

public class MongoDbAuthorizationRealm extends AuthorizingRealm {

    public static final String NAME = "mongodb-authorization-realm";
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbAuthorizationRealm.class);
    private final UserService userService;
    private final PermissionAndRoleResolver permissionAndRoleResolver;
    private final GRNRegistry grnRegistry;

    @Inject
    MongoDbAuthorizationRealm(UserService userService,
                              MongoDbAuthorizationCacheManager mongoDbAuthorizationCacheManager,
                              PermissionAndRoleResolver permissionAndRoleResolver,
                              GRNRegistry grnRegistry,
                              EventBus serverEventBus) {
        this.userService = userService;
        this.permissionAndRoleResolver = permissionAndRoleResolver;
        this.grnRegistry = grnRegistry;
        setCachingEnabled(true);
        setCacheManager(mongoDbAuthorizationCacheManager);
        serverEventBus.register(this);
    }

    @Override
    protected Object getAuthorizationCacheKey(PrincipalCollection principals) {
        final Optional<String> requestId = ShiroRequestHeadersBinder.getHeaderFromThreadContext(X_REQUEST_ID);

        if (requestId.isPresent()) {
            return ImmutableSet.builder().addAll(principals).add(requestId.get()).build();
        }
        LOG.warn("Could not find X-Request-Id header. This is not supposed to happen.");
        return principals.asSet();
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        LOG.debug("Retrieving authorization information for: {}", principals);

        // This realm can handle both, user String principals and GRN principals.
        final GRN principal = getUserPrincipal(principals)
                .orElseGet(() -> getGRNPrincipal(principals).orElse(null));
        if (principal == null) {
            return new SimpleAuthorizationInfo();
        }
        LOG.debug("GRN principal: {}", principal);

        final ImmutableSet.Builder<Permission> permissionsBuilder = ImmutableSet.builder();
        final ImmutableSet.Builder<String> rolesBuilder = ImmutableSet.builder();

        // Resolve grant permissions and roles for the GRN
        permissionsBuilder.addAll(permissionAndRoleResolver.resolvePermissionsForPrincipal(principal));
        rolesBuilder.addAll(permissionAndRoleResolver.resolveRolesForPrincipal(principal));

        if (GRNTypes.USER.equals(principal.grnType())) {
            // If the principal is a user, we also need to load permissions and roles from the user object
            final User user = userService.loadById(principal.entity());
            if (user != null) {
                final Set<Permission> userPermissions = user.getObjectPermissions();

                permissionsBuilder.addAll(userPermissions);
                rolesBuilder.addAll(user.getRoleIds());
            } else {
                LOG.warn("User <{}> not found for permission and role resolving", principal);
            }
        }

        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        info.setObjectPermissions(permissionsBuilder.build());
        info.setRoles(rolesBuilder.build());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Authorization info for {} - permissions: {}", principal, info.getObjectPermissions());
            LOG.debug("Authorization info for {} - roles: {}", principal, info.getRoles());
        }

        return info;
    }

    private Optional<GRN> getUserPrincipal(PrincipalCollection principals) {
        final String userId = Iterables.getFirst(principals.byType(String.class), null);
        if (isBlank(userId)) {
            return Optional.empty();
        }
        return Optional.of(grnRegistry.newGRN("user", userId));
    }

    private Optional<GRN> getGRNPrincipal(PrincipalCollection principals) {
        final GRN principal = Iterables.getFirst(principals.byType(GRN.class), null);
        if (principal == null) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        // This class does not authenticate at all
        return false;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        // This class does not authenticate at all
        return null;
    }

    @Subscribe
    public void handleUserSave(UserChangedEvent event) {
        getAuthorizationCache().clear();
    }
}
