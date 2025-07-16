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
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNType;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.CapabilityRegistry;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDependencyPermissionChecker;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("EntitySharesServiceTest.json")
class EntitySharesServiceTest {

    private EntitySharesService entitySharesService;

    @Mock
    private EntityDependencyResolver entityDependencyResolver;

    @Mock
    private EntityDependencyPermissionChecker entityDependencyPermissionChecker;

    @Mock
    private GranteeService granteeService;

    @Mock
    private EntityCreationRequestService entityCreationRequestService;

    private GRNRegistry grnRegistry;
    private DBGrantService dbGrantService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry) {
        this.grnRegistry = grnRegistry;

        this.dbGrantService = new DBGrantService(new MongoCollections(mongoJackObjectMapperProvider, mongodb.mongoConnection()));

        lenient().when(entityDependencyResolver.resolve(any())).thenReturn(ImmutableSet.of());
        lenient().when(entityDependencyPermissionChecker.check(any(), any(), any())).thenReturn(ImmutableMultimap.of());
        lenient().when(granteeService.getAvailableGrantees(any())).thenReturn(ImmutableSet.of());

        final EventBus serverEventBus = mock(EventBus.class);
        this.entitySharesService = new EntitySharesService(
                dbGrantService, entityDependencyResolver, entityDependencyPermissionChecker,
                grnRegistry, granteeService, entityCreationRequestService, serverEventBus,
                new HashSet<>(), new CapabilityRegistry(grnRegistry, Set.of(new RestPermissions(), new RestPermissions())));
    }

    @DisplayName("Validates we cannot remove the last owner")
    @Test
    void validateLastOwnerCannotBeRemoved() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of());

        // This test can also see the "invisible user"
        final Set<GRN> allGrantees = dbGrantService.getAll().stream().map(GrantDTO::grantee).collect(Collectors.toSet());
        final Set<Grantee> allGranteesSet = allGrantees.stream().map(g -> Grantee.createUser(g, g.entity())).collect(Collectors.toSet());
        lenient().when(granteeService.getAvailableGrantees(any())).thenReturn(
                allGranteesSet
        );
        lenient().when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);

        final User user = createMockUser("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isTrue();
            assertThat(validationResult.getErrors()).isNotEmpty();
            assertThat(validationResult.getErrors().get(EntityShareRequest.SELECTED_GRANTEE_CAPABILITIES).toString())
                    .contains("Removing the following owners")
                    .contains("grn::::user:jane")
                    .contains("grn::::user:invisible");
        });
    }

    @DisplayName("The validation should ignore invisble owners")
    @Test
    void ignoreInvisibleOwners() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of());

        final Set<GRN> allGrantees = dbGrantService.getAll().stream().map(GrantDTO::grantee).collect(Collectors.toSet());
        final Set<Grantee> allGranteesSet = allGrantees.stream()
                .filter(g -> g.toString().equals("grn::::user:invisible"))
                .map(g -> Grantee.createUser(g, g.entity())).collect(Collectors.toSet());
        lenient().when(granteeService.getAvailableGrantees(any())).thenReturn(
                allGranteesSet
        );
        lenient().when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);

        final User user = createMockUser("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isFalse();
        });
    }

    @DisplayName("Validates we cannot remove the last owner by changing the own capability")
    @Test
    void validateLastOwnerCannotBeRemovedByChangingCapability() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.EVENT_DEFINITION, "54e3deadbeefdeadbeefaffe");
        final GRN bob = grnRegistry.newGRN(GRNTypes.USER, "bob");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of(bob, Capability.VIEW));

        final User user = createMockUser("requestingUser");
        final ImmutableSet<Grantee> allGranteesSet = ImmutableSet.of(Grantee.createUser(bob, "bob"));
        when(granteeService.getAvailableGrantees(user)).thenReturn(allGranteesSet);
        lenient().when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);

        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isTrue();
            assertThat(validationResult.getErrors()).isNotEmpty();
            assertThat(validationResult.getErrors().get(EntityShareRequest.SELECTED_GRANTEE_CAPABILITIES))
                    .contains("Removing the following owners <[grn::::user:bob]> will leave the entity ownerless.");
        });
    }

    @DisplayName("Validates we can switch owners")
    @Test
    void validateOwnerSwitch() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final GRN horst = grnRegistry.newGRN(GRNTypes.USER, "horst");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of(horst, Capability.OWN));

        final User user = createMockUser("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isFalse();
            assertThat(validationResult.getErrors()).isEmpty();
        });
    }

    @DisplayName("Validates we can modify ownerless entitites")
    @Test
    void validateOwnerless() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "54e3deadbeefdeadbeefaffe");
        final GRN horst = grnRegistry.newGRN(GRNTypes.USER, "horst");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of(horst, Capability.MANAGE));

        final User user = createMockUser("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isFalse();
            assertThat(validationResult.getErrors()).isEmpty();
        });
    }

    @DisplayName("Don't run validation on initial empty request")
    @Test
    void noValidationOnEmptyRequest() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "54e3deadbeefdeadbeefaffe");
        final EntityShareRequest shareRequest = EntityShareRequest.create(null);

        final User user = createMockUser("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isFalse();
            assertThat(validationResult.getErrors()).isEmpty();
        });
    }

    @DisplayName("Only show shares for visible grantees")
    @Test
    void noSharesforInvisibleGrantees() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final EntityShareRequest shareRequest = EntityShareRequest.create(null);

        final User user = createMockUser("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.activeShares()).satisfies(activeShares -> {
            assertThat(activeShares).isEmpty();
        });

    }

    @DisplayName("Only show shares for visible grantees")
    @Test
    void showShareForVisibleGrantee() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final EntityShareRequest shareRequest = EntityShareRequest.create(null);

        final User user = createMockUser("hans");
        final GRN janeGRN = grnRegistry.newGRN(GRNTypes.USER, "jane");
        final ImmutableSet<Grantee> allGranteesSet = ImmutableSet.of(Grantee.createUser(janeGRN, "jane"));
        when(granteeService.getAvailableGrantees(user)).thenReturn(allGranteesSet);
        when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);

        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user);
        assertThat(entityShareResponse.activeShares()).satisfies(activeShares -> {
            assertThat(activeShares).hasSize(1);
            assertThat(activeShares.iterator().next().grantee()).isEqualTo(janeGRN);
        });
    }

    @DisplayName("Show shares without entity")
    @Test
    void showShareWithoutEntity() {
        final User user = createMockUser("hans");
        final GRN janeGRN = grnRegistry.newGRN(GRNTypes.USER, "jane");
        final ImmutableSet<Grantee> allGranteesSet = ImmutableSet.of(Grantee.createUser(janeGRN, "jane"));
        when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);

        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(user, ImmutableMap.of());

        assertThat(entityShareResponse.activeShares()).isEmpty();
        assertThat(entityShareResponse.availableGrantees()).hasSize(1);
        assertThat(entityShareResponse.availableCapabilities()).hasSize(3);
        assertThat(entityShareResponse.selectedGranteeCapabilities()).isEmpty();
    }

    @DisplayName("Clone grants along with entity")
    @Test
    void cloneGrants() {
        final GRNType grnType = GRNTypes.DASHBOARD;
        final String idOrigin = "54e3deadbeefdeadbeefaffe";
        final String idClone = "54e3deadbeefdeadbeefbeef";
        final User user = createMockUser("hans");

        final GRN originGRN = grnRegistry.newGRN(grnType, idOrigin);
        final GRN cloneGRN = grnRegistry.newGRN(grnType, idClone);
        final GRN janeGRN = grnRegistry.newGRN(GRNTypes.USER, "jane");
        final GRN bobGRN = grnRegistry.newGRN(GRNTypes.USER, "bob");

        final ImmutableSet<Grantee> allGranteesSet = ImmutableSet.of(
                Grantee.createUser(janeGRN, "jane"), Grantee.createUser(bobGRN, "bob"));
        final GrantDTO grantDTOjane = GrantDTO.builder()
                .grantee(janeGRN)
                .capability(Capability.MANAGE)
                .target(originGRN)
                .build();
        dbGrantService.create(grantDTOjane, user.getName());

        final GrantDTO grantDTObob = GrantDTO.builder()
                .grantee(bobGRN)
                .capability(Capability.VIEW)
                .target(originGRN)
                .build();
        dbGrantService.create(grantDTObob, user.getName());

        when(granteeService.getAvailableGrantees(any())).thenReturn(allGranteesSet);
        when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);

        entitySharesService.cloneEntityGrants(grnType, idOrigin, idClone, user);

        final List<GrantDTO> forTarget = dbGrantService.getForTarget(cloneGRN);
        assertThat(forTarget).extracting(GrantDTO::grantee)
                .containsExactlyInAnyOrder(janeGRN, bobGRN);
    }

    @DisplayName("Show shares with permissions check")
    @Test
    void showShareWithCheck() {
        final String STREAM_ID = "54e3deadbeefdeadbeefaffe";
        final String STREAM_GRN_STRING = "grn::::stream:" + STREAM_ID;
        final GRN STREAM_GRN = grnRegistry.newGRN(GRNTypes.STREAM, STREAM_ID);
        final User user = createMockUser("hans");
        final GRN janeGRN = grnRegistry.newGRN(GRNTypes.USER, "jane");
        final ImmutableSet<Grantee> allGranteesSet = ImmutableSet.of(Grantee.createUser(janeGRN, "jane"));
        when(granteeService.getModifiableGrantees(any(), any())).thenReturn(allGranteesSet);
        when(entityDependencyPermissionChecker.check(any(), any(), any()))
                .thenReturn(ImmutableMultimap.of(janeGRN, EntityDescriptor.create(STREAM_GRN, "stream", Set.of())));
        when(entityDependencyResolver.descriptorFromGRN(any())).thenReturn(EntityDescriptor.create(STREAM_GRN, "stream", Set.of()));

        final EntityShareResponse entityShareResponse =
                entitySharesService.prepareShare(List.of(STREAM_GRN_STRING), user);

        assertThat(entityShareResponse.availableGrantees()).containsExactly(Grantee.createUser(janeGRN, "jane"));

        assertThat(entityShareResponse.missingPermissionsOnDependencies()).hasSize(1);
        final Collection<EntityDescriptor> entityDescriptors = entityShareResponse.missingPermissionsOnDependencies().get(janeGRN);
        assertThat(entityDescriptors).hasSize(1);
        assertThat(entityDescriptors.iterator().next().id()).hasToString(STREAM_GRN_STRING);
    }

    private User createMockUser(String name) {
        final User user = mock(User.class);
        lenient().when(user.getName()).thenReturn(name);
        lenient().when(user.getId()).thenReturn(name);
        return user;
    }
}
