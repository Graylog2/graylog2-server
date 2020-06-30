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
import org.graylog.security.BuiltinRoles;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.security.shares.EntitySharePrepareResponse.ActiveShare;
import org.graylog.security.shares.EntitySharePrepareResponse.AvailableRole;
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
     * Prepares the sharing operation by running some checks and returning available roles and grantees as well as
     * active shares and information about missing dependencies.
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
                .availableGrantees(granteeService.getAvailableGrantees(sharingUser))
                .availableRoles(getAvailableRoles())
                .activeShares(activeShares)
                .selectedGranteeRoles(getSelectedGranteeRoles(activeShares, request.selectedGranteeRoles()))
                .missingDependencies(getMissingDependencies(ownedEntity, sharingUser, request.selectedGranteeRoles()))
                .build();
    }

    /**
     * Share an entity with one or more grantees.
     * The grants in the request are created or, if they already exist, updated.
     *
     * @param ownedEntity the target entity for the updated grants
     * @param request     the request containing grantees and their roles
     * @param sharingUser the user executing the request
     */
    public EntitySharePrepareResponse updateEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser) {
        final String userName = sharingUser.getName();
        final List<GrantDTO> existingGrants = grantService.getForTarget(ownedEntity, grnRegistry.newGRN("user", sharingUser.getName()));

        // update roles of existing grants (for a grantee)
        existingGrants.stream().filter(grantDTO -> request.grantees().contains(grantDTO.grantee())).forEach((g -> {
            final GRN newRole = request.granteeRoles().get(g.grantee());
            if (!g.role().equals(newRole.toString())) {
                grantService.save(g.toBuilder()
                        .role(newRole.toString())
                        .updatedBy(userName)
                        .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
                        .build());
            }
        }));

        // create newly added grants
        // TODO create multiple entries with one db query
        request.granteeRoles().forEach((grantee, role) -> {
            if (existingGrants.stream().noneMatch(eg -> eg.grantee().equals(grantee))) {
                grantService.create(GrantDTO.builder()
                                .grantee(grantee)
                                .role(role.toString())
                                .target(ownedEntity)
                                .build(),
                        sharingUser);
            }
        });

        // remove grants that are not present anymore
        // TODO delete multiple entries with one db query
        existingGrants.forEach((g) -> {
            if (!request.granteeRoles().containsKey(g.grantee())) {
                grantService.delete(g.id());
            }
        });

        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser);
        return EntitySharePrepareResponse.builder()
                .entity(ownedEntity.toString())
                .availableGrantees(granteeService.getAvailableGrantees(sharingUser))
                .availableRoles(getAvailableRoles())
                .activeShares(activeShares)
                .selectedGranteeRoles(getSelectedGranteeRoles(activeShares, request.granteeRoles()))
                .missingDependencies(getMissingDependencies(ownedEntity, sharingUser, request.granteeRoles()))
                .build();
    }

    private Map<GRN, GRN> getSelectedGranteeRoles(ImmutableSet<ActiveShare> activeShares, ImmutableMap<GRN, GRN> selectedGrantees) {
        // If the user doesn't submit a grantee selection we return the active shares as selection so the frontend
        // can just render it
        if (selectedGrantees.isEmpty()) {
            return activeShares.stream()
                    .collect(Collectors.toMap(ActiveShare::grantee, activeShare -> grnRegistry.parse(activeShare.role())));
        }
        // If the user submits a grantee selection, we only return that one because we expect the frontend to always
        // submit the full selection not only added/removed grantees.
        return selectedGrantees;
    }

    private ImmutableSet<ActiveShare> getActiveShares(GRN ownedEntity, User sharingUser) {
        final List<GrantDTO> activeGrants = grantService.getForTarget(ownedEntity, grnRegistry.newGRN("user", sharingUser.getName()));

        return activeGrants.stream()
                .map(grant -> ActiveShare.create(grnRegistry.newGRN("grant", grant.id()).toString(), grant.grantee(), grant.role()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<AvailableRole> getAvailableRoles() {
        return BuiltinRoles.allSharingRoles().stream()
                .map(role -> AvailableRole.create(grnRegistry.newGRN("role", role.id()).toString(), role.title()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<MissingDependency> getMissingDependencies(GRN ownedEntity, User sharingUser, ImmutableMap<GRN, GRN> selectedGranteeRoles) {
        // TODO: We need to compute the missing dependencies by taking the selectedGrantees into account.
        //       (e.g. missing grants for selectedGrantees on the streams required for a dashboard to work correctly)
        // TODO: We can only expose the missing dependencies that the sharing user has access to to avoid
        //       leaking information to the user.
        final ImmutableSet<MissingDependency> dependencies = entityDependencyResolver.resolve(ownedEntity);
        return dependencies.stream()
                .filter(dependency -> true) // TODO: Only return dependencies the selectedGrantees don't have access to
                .collect(ImmutableSet.toImmutableSet());
    }

}
