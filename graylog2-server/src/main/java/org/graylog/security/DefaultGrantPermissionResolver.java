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
package org.graylog.security;

import com.google.common.collect.ImmutableSet;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.graylog.security.permissions.GRNPermission;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.utilities.GRN;
import org.graylog2.utilities.GRNRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class DefaultGrantPermissionResolver implements GrantPermissionResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGrantPermissionResolver.class);

    private final Logger logger;
    private final BuiltinCapabilities builtinCapabilities;
    private final DBGrantService grantService;
    private final GRNRegistry grnRegistry;

    @Inject
    public DefaultGrantPermissionResolver(BuiltinCapabilities builtinCapabilities,
                                          DBGrantService grantService,
                                          GRNRegistry grnRegistry) {
        this(LOG, builtinCapabilities, grantService, grnRegistry);
    }

    public DefaultGrantPermissionResolver(Logger logger,
                                          BuiltinCapabilities builtinCapabilities,
                                          DBGrantService grantService,
                                          GRNRegistry grnRegistry) {
        this.logger = logger;
        this.builtinCapabilities = builtinCapabilities;
        this.grantService = grantService;
        this.grnRegistry = grnRegistry;
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

    protected Set<GRN> resolveGrantees(String userName) {
        return Collections.singleton(grnRegistry.newGRN("user", userName));
    }

    @Override
    public Set<Permission> resolvePermissionsForUser(String userName) {
        final Set<GrantDTO> grants = grantService.getForGranteesOrGlobal(resolveGrantees(userName));

        final ImmutableSet.Builder<Permission> permissionsBuilder = ImmutableSet.builder();

        for (GrantDTO grant : grants) {
            final Optional<CapabilityDescriptor> capability = builtinCapabilities.get(grant.capability());

            if (capability.isPresent()) {
                final Set<GRN> targets = resolveTargets(grant.target());

                for (String permission : capability.get().permissions()) {
                    for (GRN target : targets) {
                        if (target.isPermissionApplicable(permission)) {
                            // TODO Find a better way to distinguish between old and new types of permissions
                            // Possible solution: Don't use strings for the constants
                            if (permission.equals(RestPermissions.ENTITY_OWN)) {
                                permissionsBuilder.add(GRNPermission.create(permission, target));
                            } else {
                                permissionsBuilder.add(new WildcardPermission(permission + ":" + target.entity()));
                            }
                        }
                    }
                }
            } else {
                logger.warn("Couldn't find capability <{}>", grant.capability());
            }
        }

        return permissionsBuilder.build();
    }
}
