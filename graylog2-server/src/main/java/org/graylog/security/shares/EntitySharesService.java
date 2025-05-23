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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import jakarta.inject.Inject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDependencyPermissionChecker;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog.security.events.EntitySharesUpdateEvent;
import org.graylog.security.shares.EntityShareResponse.ActiveShare;
import org.graylog.security.shares.EntityShareResponse.AvailableCapability;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.requireNonBlank;

/**
 * Handler for sharing calls.
 */
public class EntitySharesService {
    private static final Logger LOG = LoggerFactory.getLogger(EntitySharesService.class);

    private final DBGrantService grantService;
    private final EntityDependencyResolver entityDependencyResolver;
    private final EntityDependencyPermissionChecker entityDependencyPermissionChecker;
    private final GRNRegistry grnRegistry;
    private final GranteeService granteeService;
    private final EventBus serverEventBus;
    private final Set<SyncedEntitiesResolver> entitiesResolvers;
    private final BuiltinCapabilities builtinCapabilities;

    @Inject
    public EntitySharesService(final DBGrantService grantService,
                               final EntityDependencyResolver entityDependencyResolver,
                               final EntityDependencyPermissionChecker entityDependencyPermissionChecker,
                               final GRNRegistry grnRegistry,
                               final GranteeService granteeService,
                               final EventBus serverEventBus,
                               final Set<SyncedEntitiesResolver> entitiesResolvers,
                               final BuiltinCapabilities builtinCapabilities) {
        this.grantService = grantService;
        this.entityDependencyResolver = entityDependencyResolver;
        this.entityDependencyPermissionChecker = entityDependencyPermissionChecker;
        this.grnRegistry = grnRegistry;
        this.granteeService = granteeService;
        this.serverEventBus = serverEventBus;
        this.entitiesResolvers = entitiesResolvers;
        this.builtinCapabilities = builtinCapabilities;
    }

    /**
     * Prepares the sharing operation by running some checks and returning available capabilities and grantees
     * as well as active shares and information about missing dependencies for a specific entity.
     * @param ownedEntity    the entity that should be shared and is owned by the sharing user
     * @param request        sharing request
     * @param sharingUser    the sharing user
     * @return the response
     */
    public EntityShareResponse prepareShare(GRN ownedEntity, EntityShareRequest request, User sharingUser) {
        requireNonNull(ownedEntity, "ownedEntity cannot be null");
        requireNonNull(request, "request cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");

        final GRN sharingUserGRN = grnRegistry.ofUser(sharingUser);
        final Set<Grantee> modifiableGrantees = getModifiableGrantees(sharingUser, ownedEntity);
        final Set<GRN> modifiableGranteeGRNs = modifiableGrantees.stream().map(Grantee::grn).collect(Collectors.toSet());
        final ImmutableSet<ActiveShare> modifiableActiveShares = getActiveShares(ownedEntity, sharingUser, modifiableGranteeGRNs);

        return EntityShareResponse.builder()
                .entity(ownedEntity.toString())
                .sharingUser(sharingUserGRN)
                .availableGrantees(modifiableGrantees)
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(modifiableActiveShares)
                .selectedGranteeCapabilities(getSelectedGranteeCapabilities(modifiableActiveShares, request))
                .missingPermissionsOnDependencies(checkMissingPermissionsOnDependencies(ownedEntity, sharingUserGRN, modifiableActiveShares, request))
                .validationResult(validateRequest(ownedEntity, request, sharingUser, modifiableGranteeGRNs))
                .build();
    }

    /**
     * Checks for missing permissions on multiple entities.
     * @param selectedCapabilities check these capabilities
     * @param entityGRNs           check these entities
     * @param sharingUser          the sharing user
     * @return eligible grantees and missing permissions on dependencies
     */
    public EntityShareResponse prepareShare(ImmutableMap<GRN, Capability> selectedCapabilities, List<String> entityGRNs, User sharingUser) {
        requireNonNull(selectedCapabilities, "selectedCapabilities cannot be null");
        requireNonNull(entityGRNs, "dependentEntityGRNs cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");

        final GRN sharingUserGRN = grnRegistry.ofUser(sharingUser);
        EntityShareRequest shareRequest = EntityShareRequest.create(selectedCapabilities);
        Map<GRN, Collection<EntityDescriptor>> missingPermissions = missingPermissions(shareRequest.grantees(), entityGRNs, sharingUserGRN);

        Set<Grantee> modifiableGrantees = new HashSet<>();
        entityGRNs.forEach(entity ->
                modifiableGrantees.addAll(getModifiableGrantees(sharingUser, grnRegistry.parse(entity)))
        );

        return EntityShareResponse.builder()
                .entity(null)
                .sharingUser(sharingUserGRN)
                .availableGrantees(modifiableGrantees)
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(ImmutableSet.of())
                .selectedGranteeCapabilities(selectedCapabilities)
                .missingPermissionsOnDependencies(missingPermissions)
                .validationResult(new ValidationResult())
                .build();
    }

    /**
     * Prepares the sharing operation by running some checks and returning available capabilities and grantees.
     * This method is used for generic sharing operations where the entity is not known yet - active shares and
     * dependencies are always null.
     */
    public EntityShareResponse prepareShare(User sharingUser) {
        requireNonNull(sharingUser, "sharingUser cannot be null");

        final GRN sharingUserGRN = grnRegistry.ofUser(sharingUser);
        final Set<Grantee> modifiableGrantees = getModifiableGrantees(sharingUser);

        return EntityShareResponse.builder()
                .entity(null)
                .sharingUser(sharingUserGRN)
                .availableGrantees(modifiableGrantees)
                .availableCapabilities(getAvailableCapabilities())
                .activeShares(ImmutableSet.of())
                .selectedGranteeCapabilities(ImmutableMap.of())
                .validationResult(new ValidationResult())
                .build();
    }

    /**
     * Similar to generic prepare without a specific entity; but also checks for missing permissions on specified
     * dependent entities.
     * @param entityGRNs check permissions on these entities
     * @param sharingUser         the sharing user
     * @return eligible grantees and missing permissions on dependencies
     */
    public EntityShareResponse prepareShare(List<String> entityGRNs, User sharingUser) {
        requireNonNull(entityGRNs, "entityGRNs cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");
        final EntityShareResponse response = prepareShare(sharingUser);

        return response.toBuilder()
                .missingPermissionsOnDependencies(missingPermissions(
                        response.availableGrantees().stream().map(Grantee::grn).collect(Collectors.toSet()),
                        entityGRNs, response.sharingUser()))
                .build();
    }

    private Map<GRN, Collection<EntityDescriptor>> missingPermissions(Set<GRN> selectedGrantees, List<String> entityGRNs, GRN sharingUserGRN) {
        final ImmutableSet<EntityDescriptor> entities = entityGRNs.stream()
                .map(grn -> EntityDescriptor.create(grnRegistry.parse(grn), grn, Set.of()))
                .collect(ImmutableSet.toImmutableSet());

        final ImmutableMultimap<GRN, EntityDescriptor> deniedEntities =
                entityDependencyPermissionChecker.check(sharingUserGRN, entities, selectedGrantees);

        Map<GRN, Collection<EntityDescriptor>> missingPermissions = new HashMap<>();
        deniedEntities.keySet().forEach(grantee -> {
            final ImmutableSet<EntityDescriptor> missing = deniedEntities.get(grantee).stream().collect(ImmutableSet.toImmutableSet());
            missingPermissions.put(grantee, missing);
        });
        return missingPermissions;
    }

    private Set<Grantee> getModifiableGrantees(User sharingUser, GRN ownedEntity) {
        final Set<Grantee> availableGrantees = granteeService.getAvailableGrantees(sharingUser);
        final Set<GRN> availableGranteeGRNs = availableGrantees.stream().map(Grantee::grn).collect(Collectors.toSet());
        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser, availableGranteeGRNs);
        return granteeService.getModifiableGrantees(availableGrantees, activeShares);
    }

    private Set<Grantee> getModifiableGrantees(User sharingUser) {
        final Set<Grantee> availableGrantees = granteeService.getAvailableGrantees(sharingUser);
        return granteeService.getModifiableGrantees(availableGrantees, null);
    }

    /**
     * Share / unshare an entity with one or more grantees.
     *
     * @param grnType     entity type
     * @param id          entity id
     * @param request     the request containing grantees and their capabilities
     * @param sharingUser the user executing the request
     */
    public EntityShareResponse updateEntityShares(GRNType grnType, String id, EntityShareRequest request, User sharingUser) {
        requireNonBlank(id, "entity ID cannot be null or empty");
        return updateEntityShares(grnRegistry.newGRN(grnType, id), request, sharingUser);
    }

    /**
     * Share / unshare an entity with one or more grantees.
     * The grants in the request are created or, if they already exist, updated. Any synced entities - as
     * provided by the {@link SyncedEntitiesResolver} - are also updated.
     *
     * @param ownedEntity the target entity for the updated grants
     * @param request     the request containing grantees and their capabilities
     * @param sharingUser the user executing the request
     */
    public EntityShareResponse updateEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser) {
        final EntityShareResponse result = updatePrimaryEntityShares(ownedEntity, request, sharingUser);
        return result.toBuilder().syncedEntities(
                        resolveImplicitGrants(ownedEntity, request, sharingUser))
                .build();
    }

    private EntityShareResponse updatePrimaryEntityShares(GRN ownedEntity, EntityShareRequest request, User sharingUser) {
        requireNonNull(ownedEntity, "ownedEntity cannot be null");
        requireNonNull(request, "request cannot be null");
        requireNonNull(sharingUser, "sharingUser cannot be null");

        final ImmutableMap<GRN, Capability> selectedGranteeCapabilities = request.selectedGranteeCapabilities()
                .orElse(ImmutableMap.of());

        final String userName = sharingUser.getName();
        final GRN sharingUserGRN = grnRegistry.ofUser(sharingUser);
        final Set<Grantee> modifiableGrantees = getModifiableGrantees(sharingUser, ownedEntity);
        final Set<GRN> modifiableGranteeGRNs = modifiableGrantees.stream().map(Grantee::grn).collect(Collectors.toSet());

        final List<GrantDTO> existingGrants = grantService.getForTargetExcludingGrantee(ownedEntity, sharingUserGRN);
        existingGrants.removeIf(grant -> !modifiableGranteeGRNs.contains(grant.grantee()));

        final EntityShareResponse.Builder responseBuilder = EntityShareResponse.builder()
                .entity(ownedEntity.toString())
                .sharingUser(sharingUserGRN)
                .availableGrantees(modifiableGrantees)
                .availableCapabilities(getAvailableCapabilities())
                .missingPermissionsOnDependencies(checkMissingPermissionsOnDependencies(ownedEntity, sharingUserGRN, ImmutableSet.of(), request));

        final EntitySharesUpdateEvent.Builder updateEventBuilder = EntitySharesUpdateEvent.builder()
                .user(sharingUser)
                .entity(ownedEntity);

        // Abort if validation fails, but try to return a complete EntityShareResponse
        final ValidationResult validationResult = validateRequest(ownedEntity, request, sharingUser, modifiableGranteeGRNs);
        if (validationResult.failed()) {
            final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser, modifiableGranteeGRNs);
            return responseBuilder
                    .activeShares(activeShares)
                    .selectedGranteeCapabilities(getSelectedGranteeCapabilities(activeShares, request))
                    .validationResult(validationResult)
                    .build();
        }

        // Update capabilities of existing grants (for a grantee)
        existingGrants.stream().filter(grantDTO -> request.grantees().contains(grantDTO.grantee())).forEach(g -> {
            final Capability newCapability = selectedGranteeCapabilities.get(g.grantee());
            if (!g.capability().equals(newCapability)) {
                grantService.save(g.toBuilder()
                        .capability(newCapability)
                        .updatedBy(userName)
                        .updatedAt(ZonedDateTime.now(ZoneOffset.UTC))
                        .build());
                updateEventBuilder.addUpdates(g.grantee(), newCapability, g.capability());
            }
        });

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
                updateEventBuilder.addCreates(grantee, capability);
            }
        });

        // remove grants that are not present anymore
        // TODO delete multiple entries with one db query
        existingGrants.forEach(g -> {
            if (!selectedGranteeCapabilities.containsKey(g.grantee())) {
                grantService.delete(g.id());
                updateEventBuilder.addDeletes(g.grantee(), g.capability());
            }
        });

        postUpdateEvent(updateEventBuilder.build());

        final ImmutableSet<ActiveShare> activeShares = getActiveShares(ownedEntity, sharingUser, modifiableGranteeGRNs);
        return responseBuilder
                .activeShares(activeShares)
                .selectedGranteeCapabilities(getSelectedGranteeCapabilities(activeShares, request))
                .build();
    }

    /**
     * Applies the share request to related entities, that we want to keep in sync.
     *
     * @param ownedEntity the parent entity
     * @param shareRequest the sharing request to apply to the related entities
     * @param sharingUser the sharing user
     * @return list of synced entities
     */
    private Set<GRN> resolveImplicitGrants(GRN ownedEntity, EntityShareRequest shareRequest, User sharingUser) {
        Set<GRN> syncedEntities = entitiesResolvers.stream()
                .flatMap(resolver -> resolver.syncedEntities(ownedEntity).stream())
                .collect(Collectors.toSet());

        ImmutableSet.Builder<GRN> failureBuilder = ImmutableSet.builder();
        syncedEntities.forEach(grn -> {
            final EntityShareResponse response = updatePrimaryEntityShares(grn, shareRequest, sharingUser);
            if (response.validationResult().failed()) {
                failureBuilder.add(grn);
            }
        });

        final ImmutableSet<GRN> failedEntities = failureBuilder.build();
        if (!failedEntities.isEmpty()) {
            LOG.warn("Failed to sync sharing to the following related entities: {}", failedEntities);
        }

        return syncedEntities;
    }

    /**
     * Add all grants of the original entity to the cloned entity, if they are visible to the sharing user.
     */
    public EntityShareResponse cloneEntityGrants(GRNType grnType, String idOrigin, String idClone, User sharingUser) {
        requireNonBlank(idOrigin, "original entity ID cannot be null or empty");
        requireNonBlank(idClone, "cloned entity ID cannot be null or empty");
        requireNonNull(sharingUser, "sharingUser cannot be null");
        final GRN grnOrigin = grnRegistry.newGRN(grnType, idOrigin);
        final GRN grnClone = grnRegistry.newGRN(grnType, idClone);

        final Set<GRN> modifiableGranteeGRNs = getModifiableGrantees(sharingUser, grnOrigin)
                .stream().map(Grantee::grn).collect(Collectors.toSet());
        final List<GrantDTO> existingGrants = grantService.getForTarget(grnOrigin);

        EntityShareRequest shareRequest = EntityShareRequest.create(
                existingGrants.stream()
                        .filter(grant -> modifiableGranteeGRNs.contains(grant.grantee()))
                        .collect(Collectors.toMap(GrantDTO::grantee, GrantDTO::capability))
        );

        return updateEntityShares(grnClone, shareRequest, sharingUser);
    }

    private void postUpdateEvent(EntitySharesUpdateEvent updateEvent) {
        this.serverEventBus.post(updateEvent);
    }

    private ValidationResult validateRequest(GRN ownedEntity, EntityShareRequest request, User sharingUser, Set<GRN> availableGranteeGRNs) {
        final ValidationResult validationResult = new ValidationResult();

        final List<GrantDTO> allEntityGrants = grantService.getForTarget(ownedEntity);
        final List<GrantDTO> existingGrants = grantService.getForTargetExcludingGrantee(ownedEntity, grnRegistry.ofUser(sharingUser));

        // The initial request doesn't submit a grantee selection. Just return.
        if (request.selectedGranteeCapabilities().isEmpty()) {
            return validationResult;
        }

        final ImmutableMap<GRN, Capability> selectedGranteeCapabilities = request.selectedGranteeCapabilities().get();

        // If there is still an owner in the selection, everything is fine
        if (selectedGranteeCapabilities.containsValue(Capability.OWN)) {
            return validationResult;
        }
        // If this entity is already ownerless, things can't get any worse. Let this request pass.
        if (allEntityGrants.stream().noneMatch(g -> g.capability().equals(Capability.OWN))) {
            return validationResult;
        }

        // Iterate over all existing owner grants and find modifications
        ArrayList<GRN> removedOwners = new ArrayList<>();
        existingGrants.stream().filter(g -> g.capability().equals(Capability.OWN)).forEach(g -> {
            // owner got removed
            if (!selectedGranteeCapabilities.containsKey(g.grantee())) {
                // Ignore owners that were invisible to the requesting user
                if (availableGranteeGRNs.contains(g.grantee())) {
                    removedOwners.add(g.grantee());
                }
                // owner capability got changed
            } else if (!selectedGranteeCapabilities.get(g.grantee()).equals(Capability.OWN)) {
                removedOwners.add(g.grantee());
            }
        });

        // If all removedOwners are applied, is there still at least one owner left?
        if (allEntityGrants.stream().filter(g -> g.capability().equals(Capability.OWN))
                .map(GrantDTO::grantee).anyMatch(grantee -> !removedOwners.contains(grantee))) {
            return validationResult;
        }
        validationResult.addError(EntityShareRequest.SELECTED_GRANTEE_CAPABILITIES,
                String.format(Locale.US, "Removing the following owners <%s> will leave the entity ownerless.", removedOwners));
        // Also return the grantees as list to be used by the frontend
        validationResult.addContext(EntityShareRequest.SELECTED_GRANTEE_CAPABILITIES,
                removedOwners.stream().map(Objects::toString).collect(Collectors.toSet()));

        return validationResult;
    }

    private Map<GRN, Capability> getSelectedGranteeCapabilities(ImmutableSet<ActiveShare> activeShares, EntityShareRequest shareRequest) {
        // If the user doesn't submit a grantee selection we return the active shares as selection so the frontend
        // can just render it
        if (shareRequest.selectedGranteeCapabilities().isEmpty()) {
            return activeShares.stream()
                    .collect(Collectors.toMap(ActiveShare::grantee, ActiveShare::capability));
        }
        // If the user submits a grantee selection, we only return that one because we expect the frontend to always
        // submit the full selection not only added/removed grantees. If the grantee selection is empty, that means
        // all shares should be removed.
        return shareRequest.selectedGranteeCapabilities().get();
    }

    private ImmutableSet<ActiveShare> getActiveShares(GRN ownedEntity, User sharingUser, Set<GRN> availableGranteeGRNs) {
        final List<GrantDTO> activeGrants = grantService.getForTargetExcludingGrantee(ownedEntity, grnRegistry.ofUser(sharingUser));

        return activeGrants.stream()
                .filter(grant -> availableGranteeGRNs.contains(grant.grantee()))
                .map(grant -> ActiveShare.create(grnRegistry.newGRN("grant", grant.id()).toString(), grant.grantee(), grant.capability()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableSet<AvailableCapability> getAvailableCapabilities() {
        // TODO: Don't use GRNs for capabilities
        return builtinCapabilities.allSharingCapabilities().stream()
                .map(descriptor -> EntityShareResponse.AvailableCapability.create(descriptor.capability().toId(), descriptor.title()))
                .collect(ImmutableSet.toImmutableSet());
    }

    private ImmutableMap<GRN, Collection<EntityDescriptor>> checkMissingPermissionsOnDependencies(GRN entity, GRN sharingUser, ImmutableSet<ActiveShare> activeShares, EntityShareRequest shareRequest) {
        // In the initial request, the user doesn't submit a grantee selection
        // We need to use the active shares to check the dependency permissions
        Set<GRN> selectedGrantees;
        if (shareRequest.selectedGranteeCapabilities().isEmpty()) {
            selectedGrantees = activeShares.stream().map(ActiveShare::grantee).collect(Collectors.toSet());
        } else {
            selectedGrantees = shareRequest.selectedGranteeCapabilities()
                    .orElse(ImmutableMap.of()).keySet();
        }
        final ImmutableSet<EntityDescriptor> dependencies = entityDependencyResolver.resolve(entity);
        return entityDependencyPermissionChecker.check(sharingUser, dependencies, selectedGrantees).asMap();
    }
}
