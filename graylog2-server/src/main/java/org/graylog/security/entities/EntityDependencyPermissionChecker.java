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
package org.graylog.security.entities;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.Capability;
import org.graylog.security.CapabilityDescriptor;
import org.graylog.security.UserAuthorizer;
import org.graylog2.utilities.GRN;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Optional;

public class EntityDependencyPermissionChecker {
    private final UserAuthorizer.Factory userAuthorizerFactory;
    private final BuiltinCapabilities builtinCapabilities;

    @Inject
    public EntityDependencyPermissionChecker(UserAuthorizer.Factory userAuthorizerFactory,
                                             BuiltinCapabilities builtinCapabilities) {
        this.userAuthorizerFactory = userAuthorizerFactory;
        this.builtinCapabilities = builtinCapabilities;
    }

    /**
     * Runs permission checks for the given dependencies for every selected grantee and returns the entities that
     * grantees cannot access.
     *
     * @param sharingUser      the sharing user
     * @param dependencies     the dependencies to check
     * @param selectedGrantees the selected grantees
     * @return dependencies that grantees cannot access, grouped by grantee
     */
    public ImmutableMultimap<GRN, EntityDependency> check(GRN sharingUser,
                                                          ImmutableSet<EntityDependency> dependencies,
                                                          ImmutableSet<GRN> selectedGrantees) {
        final ImmutableMultimap.Builder<GRN, EntityDependency> deniedDependencies = ImmutableMultimap.builder();
        final UserAuthorizer sharerAuthorizer = userAuthorizerFactory.create(sharingUser);

        for (final GRN grantee : selectedGrantees) {
            // We only check for existing grants for the actual grantee. If the grantee is a team, we only check if
            // the team has a grant, not if any users in the team can access the dependency via other grants.
            // The same for the "everyone" grantee, we only check if  the "everyone" grantee has access to a dependency.
            final UserAuthorizer granteeAuthorizer = userAuthorizerFactory.create(grantee);

            for (final EntityDependency dependency : dependencies) {
                // We can only expose missing dependencies that the sharing user can read to avoid
                // leaking information to the sharing user.
                if (!canView(sharerAuthorizer, dependency)) {
                    continue;
                }

                if (!canView(granteeAuthorizer, dependency)) {
                    deniedDependencies.put(grantee, dependency);
                }
            }
        }

        return deniedDependencies.build();
    }

    private boolean canView(UserAuthorizer authorizer, EntityDependency dependency) {
        final Optional<CapabilityDescriptor> capabilityDescriptor = builtinCapabilities.get(Capability.VIEW);

        return capabilityDescriptor.map(CapabilityDescriptor::permissions)
                .orElse(Collections.emptySet())
                .stream()
                .filter(permission -> dependency.id().isPermissionApplicable(permission))
                .anyMatch(permission -> authorizer.isPermitted(permission, dependency.id()));
    }
}
