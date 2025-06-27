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
package org.graylog.security;

import com.google.common.collect.ImmutableSet;
import jakarta.inject.Inject;
import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class DefaultPermissionAndRoleResolver implements PermissionAndRoleResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultPermissionAndRoleResolver.class);

    private final Logger logger;
    private final CapabilityRegistry capabilityRegistry;
    private final DBGrantService grantService;

    @Inject
    public DefaultPermissionAndRoleResolver(CapabilityRegistry capabilityRegistry,
                                            DBGrantService grantService) {
        this(LOG, capabilityRegistry, grantService);
    }

    public DefaultPermissionAndRoleResolver(Logger logger,
                                            CapabilityRegistry capabilityRegistry,
                                            DBGrantService grantService) {
        this.logger = logger;
        this.capabilityRegistry = capabilityRegistry;
        this.grantService = grantService;
    }

    protected Set<GRN> resolveTargets(GRN target) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (target.type()) {
            case "system":
                // TODO
                return Collections.emptySet();
            default: // any other single entity
                return Collections.singleton(target);
        }
    }

    @Override
    public Set<GRN> resolveGrantees(GRN principal) {
        return Collections.singleton(principal);
    }

    @Override
    public Set<Permission> resolvePermissionsForPrincipal(GRN principal) {
        final Set<GrantDTO> grants = grantService.getForGranteesOrGlobal(resolveGrantees(principal));

        final ImmutableSet.Builder<Permission> permissionsBuilder = ImmutableSet.builder();

        for (GrantDTO grant : grants) {
            final Set<GRN> targets = resolveTargets(grant.target());

            for (GRN target : targets) {
                final Optional<CapabilityDescriptor> capability = capabilityRegistry.get(grant.capability());

                if (capability.isPresent()) {
                    capability.get()
                            .permissionsFor(target.grnType())
                            .forEach(permission -> permissionsBuilder.add(permission.toShiroPermission(target)));
                } else {
                    logger.warn("Couldn't find capability <{}>", grant.capability());
                }
            }
        }

        return permissionsBuilder.build();
    }

    @Override
    public Set<String> resolveRolesForPrincipal(GRN principal) {
        return ImmutableSet.of();
    }
}
