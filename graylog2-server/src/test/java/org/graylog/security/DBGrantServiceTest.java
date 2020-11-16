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
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DBGrantServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private DBGrantService dbService;
    private GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @Before
    public void setUp() throws Exception {
        final MongoJackObjectMapperProvider mapper = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        this.dbService = new DBGrantService(mongodb.mongoConnection(), mapper, grnRegistry);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void test() {
        assertThat(dbService.streamAll().collect(Collectors.toSet()).size()).isEqualTo(6);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForGranteesOrGlobal() {
        final GRN jane = grnRegistry.newGRN("user", "jane");
        final GRN john = grnRegistry.newGRN("user", "john");

        assertThat(dbService.getForGranteesOrGlobal(Collections.singleton(jane))).hasSize(4);
        assertThat(dbService.getForGranteesOrGlobal(Collections.singleton(john))).hasSize(3);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForGrantee() {
        final GRN jane = grnRegistry.newGRN("user", "jane");
        final GRN john = grnRegistry.newGRN("user", "john");

        assertThat(dbService.getForGrantee(jane)).hasSize(3);
        assertThat(dbService.getForGrantee(john)).hasSize(2);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForGranteeWithCapability() {
        final GRN jane = grnRegistry.newGRN("user", "jane");
        final GRN john = grnRegistry.newGRN("user", "john");

        assertThat(dbService.getForGranteeWithCapability(jane, Capability.MANAGE)).hasSize(1);
        assertThat(dbService.getForGranteeWithCapability(jane, Capability.OWN)).hasSize(1);
        assertThat(dbService.getForGranteeWithCapability(john, Capability.VIEW)).hasSize(1);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForGranteesOrGlobalWithCapability() {
        final GRN jane = grnRegistry.newGRN("user", "jane");
        final GRN john = grnRegistry.newGRN("user", "john");

        assertThat(dbService.getForGranteesOrGlobalWithCapability(ImmutableSet.of(jane), Capability.MANAGE)).hasSize(1);
        assertThat(dbService.getForGranteesOrGlobalWithCapability(ImmutableSet.of(jane), Capability.OWN)).hasSize(1);
        assertThat(dbService.getForGranteesOrGlobalWithCapability(ImmutableSet.of(john), Capability.VIEW)).hasSize(2);
        assertThat(dbService.getForGranteesOrGlobalWithCapability(ImmutableSet.of(jane, john), Capability.VIEW)).hasSize(3);
        assertThat(dbService.getForGranteesOrGlobalWithCapability(ImmutableSet.of(jane, john), Capability.MANAGE)).hasSize(1);
        assertThat(dbService.getForGranteesOrGlobalWithCapability(ImmutableSet.of(jane, john), Capability.OWN)).hasSize(2);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForTarget() {
        final GRN stream1 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0000");
        final GRN stream2 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0001");

        assertThat(dbService.getForTarget(stream1)).hasSize(1);
        assertThat(dbService.getForTarget(stream2)).hasSize(3);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getForTargetExcludingGrantee() {
        final GRN stream = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0001");
        final GRN grantee = grnRegistry.parse("grn::::user:john");

        assertThat(dbService.getForTargetExcludingGrantee(stream, grantee)).hasSize(2);
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void getOwnersForTargets() {
        final GRN jane = grnRegistry.parse("grn::::user:jane");
        final GRN john = grnRegistry.parse("grn::::user:john");

        final GRN dashboard1 = grnRegistry.parse("grn::::dashboard:54e3deadbeefdeadbeef0000");
        final GRN dashboard2 = grnRegistry.parse("grn::::dashboard:54e3deadbeefdeadbeef0001");
        final GRN stream1 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0001");

        assertThat(dbService.getOwnersForTargets(ImmutableSet.of(dashboard1, dashboard2, stream1))).satisfies(result -> {
            assertThat(result.get(dashboard1)).containsExactlyInAnyOrder(jane);
            assertThat(result.get(dashboard2)).containsExactlyInAnyOrder(john);
            assertThat(result).doesNotContainKey(stream1);
        });
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void hasGrantFor() {
        final GRN jane = grnRegistry.parse("grn::::user:jane");
        final GRN dashboard1 = grnRegistry.parse("grn::::dashboard:54e3deadbeefdeadbeef0000");
        final GRN stream1 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0000");
        final GRN stream2 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0001");

        assertThat(dbService.hasGrantFor(jane, Capability.VIEW, stream1)).isTrue();
        assertThat(dbService.hasGrantFor(jane, Capability.MANAGE, stream2)).isTrue();
        assertThat(dbService.hasGrantFor(jane, Capability.OWN, dashboard1)).isTrue();

        assertThat(dbService.hasGrantFor(jane, Capability.MANAGE, stream1)).isFalse();
        assertThat(dbService.hasGrantFor(jane, Capability.VIEW, dashboard1)).isFalse();
    }

    @Test
    @MongoDBFixtures("grants.json")
    public void ensure() {
        final GRN jane = grnRegistry.parse("grn::::user:jane");
        final GRN stream1 = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0000");
        final GRN newStream = grnRegistry.parse("grn::::stream:54e3deadbeefdeadbeef0888");

        // Matches existing grant. Returns original
        final GrantDTO stream1Grant = dbService.getForTargetAndGrantee(stream1, jane).get(0);
        GrantDTO result = dbService.ensure(jane, Capability.VIEW, stream1, "admin");
        assertThat(result).isEqualTo(stream1Grant);

        // Updates to a higher capability
        result = dbService.ensure(jane, Capability.MANAGE, stream1, "admin");
        assertThat(result.capability()).isEqualTo(Capability.MANAGE);

        // Don't downgrade to a lower capability
        result = dbService.ensure(jane, Capability.VIEW, stream1, "admin");
        assertThat(result.capability()).isEqualTo(Capability.MANAGE);

        // Create a new grant
        assertThat(dbService.ensure(jane, Capability.MANAGE, newStream, "admin")).isNotNull();
        assertThat(dbService.getForTarget(newStream)).satisfies(grantDTOS -> {
            assertThat(grantDTOS.size()).isEqualTo(1);
            assertThat(grantDTOS.get(0).grantee()).isEqualTo(jane);
            assertThat(grantDTOS.get(0).capability()).isEqualTo(Capability.MANAGE);
            assertThat(grantDTOS.get(0).target()).isEqualTo(newStream);
        });
    }

    @Test
    public void createWithGrantDTOAndUserObject() {
        final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        final GRN grantee = GRNTypes.USER.toGRN("jane");
        final GRN target = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0000");
        final User user = mock(User.class);

        when(user.getName()).thenReturn("john");

        final GrantDTO grantDTO = GrantDTO.of(grantee, Capability.OWN, target).toBuilder()
                // Ensure that the time tests work
                .createdAt(now.minusHours(1))
                .updatedAt(now.minusHours(1))
                .build();
        final GrantDTO grant = dbService.create(grantDTO, user);

        assertThat(grant.id()).isNotBlank();
        assertThat(grant.grantee()).isEqualTo(grantee);
        assertThat(grant.capability()).isEqualTo(Capability.OWN);
        assertThat(grant.target()).isEqualTo(target);
        assertThat(grant.createdBy()).isEqualTo("john");
        assertThat(grant.createdAt()).isAfter(grantDTO.createdAt());
        assertThat(grant.updatedBy()).isEqualTo("john");
        assertThat(grant.updatedAt()).isAfter(grantDTO.updatedAt());
    }

    @Test
    public void createWithGrantDTOAndUsernameString() {
        final GRN grantee = GRNTypes.USER.toGRN("jane");
        final GRN target = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0000");

        final GrantDTO grant = dbService.create(GrantDTO.of(grantee, Capability.MANAGE, target), "admin");

        assertThat(grant.id()).isNotBlank();
        assertThat(grant.grantee()).isEqualTo(grantee);
        assertThat(grant.capability()).isEqualTo(Capability.MANAGE);
        assertThat(grant.target()).isEqualTo(target);
        assertThat(grant.createdBy()).isEqualTo("admin");
        assertThat(grant.createdAt()).isBefore(ZonedDateTime.now(ZoneOffset.UTC));
        assertThat(grant.updatedBy()).isEqualTo("admin");
        assertThat(grant.updatedAt()).isBefore(ZonedDateTime.now(ZoneOffset.UTC));
    }

    @Test
    public void createWithGranteeCapabilityAndTarget() {
        final GRN grantee = GRNTypes.USER.toGRN("jane");
        final GRN target = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0000");

        final GrantDTO grant = dbService.create(grantee, Capability.MANAGE, target, "admin");

        assertThat(grant.id()).isNotBlank();
        assertThat(grant.grantee()).isEqualTo(grantee);
        assertThat(grant.capability()).isEqualTo(Capability.MANAGE);
        assertThat(grant.target()).isEqualTo(target);
        assertThat(grant.createdBy()).isEqualTo("admin");
        assertThat(grant.createdAt()).isBefore(ZonedDateTime.now(ZoneOffset.UTC));
        assertThat(grant.updatedBy()).isEqualTo("admin");
        assertThat(grant.updatedAt()).isBefore(ZonedDateTime.now(ZoneOffset.UTC));
    }
}
