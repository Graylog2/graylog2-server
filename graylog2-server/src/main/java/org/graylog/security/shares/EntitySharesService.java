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
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDependencyPermissionChecker;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog.security.shares.EntityShareResponse.ActiveShare;
import org.graylog.security.shares.EntityShareResponse.AvailableCapability;
import org.graylog2.plugin.database.users.User;

import javax.inject.Inject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collection;
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
    private final EntityDependencyPermissionChecker entityDependencyPermissionChecker;
    private final GRNRegistry grnRegistry;
    private final GranteeService granteeService;

    @Inject
    public EntitySharesService(DBGrantService grantService,
                               EntityDependencyResolver entityDependencyResolver,
                               EntityDependencyPermissionChecker entityDependencyPermissionChecker,
                               GRNRegistry grnRegistry,
                               GranteeService granteeService) {
        this.grantService = grantService;
        this.entityDependencyResolver = entityDependencyResolver;
        this.entityDependencyPermissionChecker = entityDependencyPermissionChecker;
        this.grnRegistry = grnRegistry;
        this.granteeService = granteeService;
    }

    /**
     * Prepares the sharing operation by running some checks and returning available capabilities and grantees
     * as well as active shares and information about missing dependencies.
     *
     * @param ownedEntity    the entity that should be shared and is owned by the sharing user
     * @param request        sharing request
     * @param sharingUser    the sharing user
     * @param sharingSubject the sharing subject
     * @return the response
     */
    public EntityShareResponse prepareShare(GRN ownedEntity,
                                            EntityShareRequest request,
                                            User sharingUser,
                                            Subject sharingSubject) {
        requireNonNull(ownedEntity, "ownedEntity cannot be null");
        requireNonNull(request, "request cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");
        requireNonNull(sharingSubject, "sharingSubject cannot be null");

        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser);
        final GRN sharingUserGRN = grnRegistry.newGRN("user", sharingUser.getName());
        return EntityShareResponse.builder()
                .entity(ownedEntity.toString())
                .sharingUser(sharingUserGRN)
                .availableGrantees(granteeService.getAvailableGrantees(sharingUser))
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(activeShares)
                .selectedGranteeCapabilities(getSelectedGranteeCapabilities(activeShares, request))
                .missingPermissionsOnDependencies(checkMissingPermissionsOnDependencies(ownedEntity, sharingUserGRN, request))
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
    public EntityShareResponse updateEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser) {
        requireNonNull(ownedEntity, "ownedEntity cannot be null");
        requireNonNull(request, "request cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");

        final ImmutableMap<GRN, Capability> selectedGranteeCapabilities = request.selectedGranteeCapabilities()
                .orElse(ImmutableMap.of());

        final String userName = sharingUser.getName();
        final GRN sharingUserGRN = grnRegistry.newGRN("user", sharingUser.getName());
        final List<GrantDTO> existingGrants = grantService.getForTargetExcludingGrantee(ownedEntity, sharingUserGRN);

        // Update capabilities of existing grants (for a grantee)
        existingGrants.stream().filter(grantDTO -> request.grantees().contains(grantDTO.grantee())).forEach((g -> {
            final Capability newCapability = selectedGranteeCapabilities.get(g.grantee());
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
        selectedGranteeCapabilities.forEach((grantee, capability) -> {
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
            if (!selectedGranteeCapabilities.containsKey(g.grantee())) {
                grantService.delete(g.id());
            }
        });

        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser);
        return EntityShareResponse.builder()
                .entity(ownedEntity.toString())
                .sharingUser(sharingUserGRN)
                .availableGrantees(granteeService.getAvailableGrantees(sharingUser))
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(activeShares)
                .selectedGranteeCapabilities(getSelectedGranteeCapabilities(activeShares, request))
                .missingPermissionsOnDependencies(checkMissingPermissionsOnDependencies(ownedEntity, sharingUserGRN, request))
                .build();
    }

    private Map<GRN, Capability> getSelectedGranteeCapabilities(ImmutableSet<ActiveShare> activeShares, EntityShareRequest shareRequest) {
        // If the user doesn't submit a grantee selection we return the active shares as selection so the frontend
        // can just render it
        if (!shareRequest.selectedGranteeCapabilities().isPresent()) {
            return activeShares.stream()
                    .collect(Collectors.toMap(ActiveShare::grantee, ActiveShare::capability));
        }
        // If the user submits a grantee selection, we only return that one because we expect the frontend to always
        // submit the full selection not only added/removed grantees. If the grantee selection is empty, that means
        // all shares should be removed.
        return shareRequest.selectedGranteeCapabilities().get();
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
                .map(descriptor -> EntityShareResponse.AvailableCapability.create(descriptor.capability().toId(), descriptor.title()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableMap<GRN, Collection<EntityDescriptor>> checkMissingPermissionsOnDependencies(GRN entity, GRN sharingUser, EntityShareRequest shareRequest) {
        final ImmutableSet<EntityDescriptor> dependencies = entityDependencyResolver.resolve(entity);
        final ImmutableSet<GRN> selectedGrantees = shareRequest.selectedGranteeCapabilities()
                .orElse(ImmutableMap.of()).keySet();
        return entityDependencyPermissionChecker.check(sharingUser, dependencies, selectedGrantees).asMap();
    }
}
