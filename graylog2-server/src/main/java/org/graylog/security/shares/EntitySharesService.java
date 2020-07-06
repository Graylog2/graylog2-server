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
package org.graylog.security.shares;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.security.shares.EntitySharePrepareResponse.ActiveShare;
import org.graylog.security.shares.EntitySharePrepareResponse.AvailableCapability;
import org.graylog.security.shares.EntitySharePrepareResponse.MissingDependency;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;
import org.graylog2.utilities.GRN;
import org.graylog2.utilities.GRNRegistry;

import javax.inject.Inject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Handler for sharing calls.
 */
public class EntitySharesService {
    private final DBGrantService grantService;
    private final EntityDependencyResolver entityDependencyResolver;
    private final UserService userService;
    private final GRNRegistry grnRegistry;
    private final GranteeService granteeService;

    @Inject
    public EntitySharesService(DBGrantService grantService,
                               EntityDependencyResolver entityDependencyResolver,
                               UserService userService,
                               GRNRegistry grnRegistry,
                               GranteeService granteeService) {
        this.grantService = grantService;
        this.entityDependencyResolver = entityDependencyResolver;
        this.userService = userService;
        this.grnRegistry = grnRegistry;
        this.granteeService = granteeService;
    }

    /**
     * Prepares the sharing operation by running some checks and returning available capabilitites and grantees
     * as well as active shares and information about missing dependencies.
     *
     * @param ownedEntity    the entity that should be shared and is owned by the sharing user
     * @param request        sharing request
     * @param sharingUser    the sharing user
     * @param sharingSubject the sharing subject
     * @return the response
     */
    public EntitySharePrepareResponse prepareShare(GRN ownedEntity,
                                                   EntitySharePrepareRequest request,
                                                   User sharingUser,
                                                   Subject sharingSubject) {
        requireNonNull(ownedEntity, "ownedEntity cannot be null");
        requireNonNull(request, "request cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");
        requireNonNull(sharingSubject, "sharingSubject cannot be null");

        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser);
        return EntitySharePrepareResponse.builder()
                .entity(ownedEntity.toString())
                .sharingUser(grnRegistry.newGRN("user", sharingUser.getName()))
                .availableGrantees(granteeService.getAvailableGrantees(sharingUser))
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(activeShares)
                .selectedGranteeCapabilities(getSelectedGranteeCapabilities(activeShares, request.selectedGranteeCapabilities()))
                .missingDependencies(getMissingDependencies(ownedEntity, sharingUser, request.selectedGranteeCapabilities()))
                .build();
    }

    /**
     * Share an entity with one or more grantees.
     * The grants in the request are created or, if they already exist, updated.
     *
     * @param ownedEntity the target entity for the updated grants
     * @param request     the request containing grantees and their capabilities
     * @param sharingUser the user executing the request
     */
    public EntitySharePrepareResponse updateEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser) {
        requireNonNull(ownedEntity, "ownedEntity cannot be null");
        requireNonNull(request, "request cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");

        final String userName = sharingUser.getName();
        final List<GrantDTO> existingGrants = grantService.getForTargetExcludingGrantee(ownedEntity, grnRegistry.newGRN("user", sharingUser.getName()));

        // Update capabilities of existing grants (for a grantee)
        existingGrants.stream().filter(grantDTO -> request.grantees().contains(grantDTO.grantee())).forEach((g -> {
            final Capability newCapability = request.selectedGranteeCapabilities().get(g.grantee());
            if (!g.capability().equals(newCapability)) {
                grantService.save(g.toBuilder()
                        .capability(newCapability)
                        .updatedBy(userName)
                        .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
                        .build());
            }
        }));

        // Create newly added grants
        // TODO Create multiple entries with one db query
        request.selectedGranteeCapabilities().forEach((grantee, capability) -> {
            if (existingGrants.stream().noneMatch(eg -> eg.grantee().equals(grantee))) {
                grantService.create(GrantDTO.builder()
                                .grantee(grantee)
                                .capability(capability)
                                .target(ownedEntity)
                                .build(),
                        sharingUser);
            }
        });

        // remove grants that are not present anymore
        // TODO delete multiple entries with one db query
        existingGrants.forEach((g) -> {
            if (!request.selectedGranteeCapabilities().containsKey(g.grantee())) {
                grantService.delete(g.id());
            }
        });

        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser);
        return EntitySharePrepareResponse.builder()
                .entity(ownedEntity.toString())
                .sharingUser(grnRegistry.newGRN("user", sharingUser.getName()))
                .availableGrantees(granteeService.getAvailableGrantees(sharingUser))
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(activeShares)
                .selectedGranteeCapabilities(getSelectedGranteeCapabilities(activeShares, request.selectedGranteeCapabilities()))
                .missingDependencies(getMissingDependencies(ownedEntity, sharingUser, request.selectedGranteeCapabilities()))
                .build();
    }

    private Map<GRN, Capability> getSelectedGranteeCapabilities(ImmutableSet<ActiveShare> activeShares, ImmutableMap<GRN, Capability> selectedGranteeCapabilities) {
        // If the user doesn't submit a grantee selection we return the active shares as selection so the frontend
        // can just render it
        if (selectedGranteeCapabilities.isEmpty()) {
            return activeShares.stream()
                    .collect(Collectors.toMap(ActiveShare::grantee, ActiveShare::capability));
        }
        // If the user submits a grantee selection, we only return that one because we expect the frontend to always
        // submit the full selection not only added/removed grantees.
        return selectedGranteeCapabilities;
    }

    private ImmutableSet<ActiveShare> getActiveShares(GRN ownedEntity, User sharingUser) {
        final List<GrantDTO> activeGrants = grantService.getForTargetExcludingGrantee(ownedEntity, grnRegistry.newGRN("user", sharingUser.getName()));

        return activeGrants.stream()
                .map(grant -> ActiveShare.create(grnRegistry.newGRN("grant", grant.id()).toString(), grant.grantee(), grant.capability()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<AvailableCapability> getAvailableCapabilities() {
        // TODO: Don't use GRNs for capabilities
        return BuiltinCapabilities.allSharingCapabilities().stream()
                .map(descriptor -> EntitySharePrepareResponse.AvailableCapability.create(descriptor.capability().toId(), descriptor.title()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<MissingDependency> getMissingDependencies(GRN ownedEntity, User sharingUser, ImmutableMap<GRN, Capability> selectedGranteeCapabilities) {
        // TODO: We need to compute the missing dependencies by taking the selectedGranteeCapabilities into account.
        //       (e.g. missing grants for selectedGranteeCapabilities on the streams required for a dashboard to work correctly)
        // TODO: We only check for existing grants for the actual grantee. If the grantee is a team, we only check if
        //       the team has a grant, not if any users in the team can access the dependency via other grants.
        //       The same for the "everyone" grantee, we only check if  the "everyone" grantee has access to a dependency.
        // TODO: We can only expose the missing dependencies that the sharing user has access to to avoid
        //       leaking information to the user.
        final ImmutableSet<MissingDependency> dependencies = entityDependencyResolver.resolve(ownedEntity);
        return dependencies.stream()
                .filter(dependency -> true) // TODO: Only return dependencies the selectedGranteeCapabilities don't have access to
                .collect(ImmutableSet.toImmutableSet());
    }

}
