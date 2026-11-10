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
package org.graylog2.users;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nonnull;
import jakarta.validation.Validator;
import org.assertj.core.api.Assertions;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.security.Permissions;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@ExtendWith(MongoDBExtension.class)
class RoleServiceImplTest {
    @Test
    void testChangeEvents(MongoCollections mongoCollections) throws ValidationException {
        final EventsCollector eventsCollector = new EventsCollector();
        final EventBus eventBus = new EventBus();

        final ClusterEventBus clusterEventBus = new ClusterEventBus() {
            @Override
            public void post(@Nonnull Object event) {
                eventBus.post(event);
            }
        };

        eventBus.register(eventsCollector);
        final RoleService service = new RoleServiceImpl(mongoCollections, new Permissions(Collections.emptySet()), Mockito.mock(Validator.class), clusterEventBus);
        service.save(createRole("inputs_manager", "manages inputs", Set.of(RestPermissions.INPUTS_READ, RestPermissions.INPUTS_CREATE), false));
        service.save(createRole("inputs_reader", "reads inputs", Set.of(RestPermissions.INPUTS_READ), false));

        Assertions.assertThat(eventsCollector.getEvents())
                .hasSize(4) // two built-in roles, Admin and Reader + whatever we add here
                .map(RoleChangedEvent::roleName)
                .contains("Admin", "Reader", "inputs_manager", "inputs_reader");
    }

    @Test
    void loadByNamesIgnoresCasingOfRequestedNames(MongoCollections mongoCollections) throws ValidationException, NotFoundException {
        final RoleService service = createService(mongoCollections);
        service.save(createRole("Inputs_Manager", "manages inputs", Set.of(RestPermissions.INPUTS_READ, RestPermissions.INPUTS_CREATE), false));

        // The built-in roles are stored as `Admin`/`Reader`, so lowercased names must find them as well.
        Assertions.assertThat(service.loadByNames(List.of("inputs_manager", "admin", "reader")))
                .map(Role::getName)
                .containsExactlyInAnyOrder("Inputs_Manager", "Admin", "Reader");
    }

    @Test
    void loadByNamesFindsRolesRequestedWithTheirStoredCasing(MongoCollections mongoCollections) throws NotFoundException {
        final RoleService service = createService(mongoCollections);

        Assertions.assertThat(service.loadByNames(List.of("Admin")))
                .map(Role::getName)
                .containsExactly("Admin");
    }

    @Test
    void loadByNamesThrowsNotFoundExceptionForUnknownRole(MongoCollections mongoCollections) {
        final RoleService service = createService(mongoCollections);

        Assertions.assertThatThrownBy(() -> service.loadByNames(List.of("admin", "does_not_exist")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("does_not_exist");
    }

    @Test
    void loadByNamesThrowsNotFoundExceptionNamingEveryMissingRole(MongoCollections mongoCollections) throws ValidationException {
        final RoleService service = createService(mongoCollections);
        service.save(createRole("Inputs_Manager", "manages inputs", Set.of(RestPermissions.INPUTS_READ), false));

        // Two of the four requested roles exist, so the exception must name the other two - and only those.
        Assertions.assertThatThrownBy(() -> service.loadByNames(List.of("admin", "inputs_manager", "missing_one", "Missing_Two")))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("missing_one")
                .hasMessageContaining("missing_two")
                .hasMessageNotContaining("admin")
                .hasMessageNotContaining("inputs_manager");
    }

    @Test
    void loadByNamesReturnsNoRolesForEmptyRequest(MongoCollections mongoCollections) throws NotFoundException {
        final RoleService service = createService(mongoCollections);

        Assertions.assertThat(service.loadByNames(List.of())).isEmpty();
    }

    @Nonnull
    private static RoleService createService(MongoCollections mongoCollections) {
        return new RoleServiceImpl(mongoCollections, new Permissions(Collections.emptySet()),
                Mockito.mock(Validator.class), new ClusterEventBus());
    }

    @Nonnull
    private static RoleImpl createRole(String name, String description, Set<String> permissions, boolean readOnly) {
        final RoleImpl role = new RoleImpl();
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions);
        role.setReadOnly(readOnly);
        return role;
    }

    private class EventsCollector {
        private final List<RoleChangedEvent> events = new LinkedList<>();

        @Subscribe
        public void subscribe(RoleChangedEvent event) {
            this.events.add(event);
        }

        public List<RoleChangedEvent> getEvents() {
            return events;
        }
    }
}
