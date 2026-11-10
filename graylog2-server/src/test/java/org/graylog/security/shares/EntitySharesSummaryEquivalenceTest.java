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

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.Capability;
import org.graylog.security.CapabilityRegistry;
import org.graylog.security.DBGrantService;
import org.graylog.security.GranteeAuthorizer;
import org.graylog.security.entities.EntityDependencyPermissionChecker;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.TestUserService;
import org.graylog.testing.TestUserServiceExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.users.UserAndTeamsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Checks that the share summaries and grantee lists show the same grantees as the active shares of the share dialog.
 */
@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(TestUserServiceExtension.class)
@MongoDBFixtures("EntitySharesSummaryEquivalenceTest.json")
class EntitySharesSummaryEquivalenceTest {
    private static final Set<String> ENTITIES = Set.of(
            "grn::::stream:54e3deadbeefdeadbeef0a01",
            "grn::::dashboard:54e3deadbeefdeadbeef0a02",
            "grn::::stream:54e3deadbeefdeadbeef0a03");

    @Mock
    private GranteeAuthorizer.Factory granteeAuthorizerFactory;
    @Mock
    private GranteeAuthorizer authorizer;
    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private EntityDependencyResolver entityDependencyResolver;
    @Mock
    private EntityDependencyPermissionChecker entityDependencyPermissionChecker;
    @Mock
    private PluggableEntityService pluggableEntityService;

    private GRNRegistry grnRegistry;
    private TestUserService userService;
    private EntitySharesService entitySharesService;

    @BeforeEach
    void setUp(MongoCollections mongoCollections, GRNRegistry grnRegistry, TestUserService userService) {
        this.grnRegistry = grnRegistry;
        this.userService = userService;

        lenient().when(granteeAuthorizerFactory.create(any(User.class))).thenReturn(authorizer);
        lenient().when(clusterConfigService.getOrDefault(any(), any())).thenReturn(UserAndTeamsConfig.DEFAULT_VALUES);
        lenient().when(entityDependencyResolver.resolve(any())).thenReturn(ImmutableSet.of());
        lenient().when(entityDependencyPermissionChecker.check(any(), any(), any())).thenReturn(ImmutableMultimap.of());

        final DefaultGranteeService granteeService = new DefaultGranteeService(userService, grnRegistry, granteeAuthorizerFactory, clusterConfigService);
        entitySharesService = new EntitySharesService(
                new DBGrantService(mongoCollections), entityDependencyResolver, entityDependencyPermissionChecker,
                grnRegistry, granteeService, pluggableEntityService, new EventBus(),
                Set.of(), new CapabilityRegistry(grnRegistry, Set.of(new RestPermissions())));
    }

    @Test
    void adminSeesSameGranteesAsShareDialog() {
        when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(true);

        assertEquivalent(userService.load("viewer"));
    }

    @Test
    void ownerWithLimitedUserVisibilitySeesSameGranteesAsShareDialog() {
        when(authorizer.isPermitted(RestPermissions.USERS_LIST)).thenReturn(false);
        when(authorizer.isPermitted(eq(RestPermissions.USERS_READ), anyString()))
                .thenAnswer(invocation -> "alice".equals(invocation.getArgument(1)));

        assertEquivalent(userService.load("viewer"));
    }

    private void assertEquivalent(User viewer) {
        final Set<GRN> entities = ENTITIES.stream().map(grnRegistry::parse).collect(Collectors.toSet());
        final Map<GRN, ShareSummary> summaries = entitySharesService.summarize(entities, viewer);

        for (final GRN entity : entities) {
            final EntityShareResponse shareDialog = entitySharesService.prepareShare(entity, EntityShareRequest.EMPTY, viewer);
            final Set<Map.Entry<GRN, Capability>> activeShares = shareDialog.activeShares().stream()
                    .map(share -> Map.entry(share.grantee(), share.capability()))
                    .collect(Collectors.toSet());
            final Map<GRN, String> dialogTitles = shareDialog.availableGrantees().stream()
                    .collect(Collectors.toMap(Grantee::grn, Grantee::title));

            final var grantees = entitySharesService.getGrantees(entity, viewer);
            assertThat(grantees)
                    .as("grantees of %s", entity)
                    .map(share -> Map.entry(share.grantee().grn(), share.capability()))
                    .containsExactlyInAnyOrderElementsOf(activeShares);
            assertThat(grantees).allSatisfy(share ->
                    assertThat(share.grantee().title()).isEqualTo(dialogTitles.get(share.grantee().grn())));

            final ShareSummary expectedSummary = new ShareSummary(
                    count(activeShares, Capability.OWN), count(activeShares, Capability.MANAGE), count(activeShares, Capability.VIEW));
            if (activeShares.isEmpty()) {
                assertThat(summaries).as("summary of %s", entity).doesNotContainKey(entity);
            } else {
                assertThat(summaries.get(entity)).as("summary of %s", entity).isEqualTo(expectedSummary);
            }
        }
    }

    private static int count(Set<Map.Entry<GRN, Capability>> shares, Capability capability) {
        return (int) shares.stream().filter(share -> share.getValue() == capability).count();
    }
}
