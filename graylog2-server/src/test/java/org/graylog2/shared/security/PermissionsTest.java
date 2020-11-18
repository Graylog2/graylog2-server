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
package org.graylog2.shared.security;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionsTest {

    private Permissions permissions;
    private RestPermissions restPermissions;

    private static class PermissionsPluginPermissions implements PluginPermissions {
        private final Set<Permission> permission;

        public PermissionsPluginPermissions(Set<Permission> permissions) {
            this.permission = permissions;
        }

        @Override
        public Set<Permission> permissions() {
            return permission;
        }

        @Override
        public Set<Permission> readerBasePermissions() {
            return Collections.emptySet();
        }
    }


    @Before
    public void setUp() throws Exception {
        restPermissions = new RestPermissions();
        permissions = new Permissions(ImmutableSet.of(restPermissions));
    }

    @Test
    public void testPluginPermissions() throws Exception {
        final ImmutableSet<Permission> pluginPermissions = ImmutableSet.of(
                Permission.create("foo:bar", "bar"),
                Permission.create("foo:baz", "baz"),
                Permission.create("hello:world", "hello")
        );
        final PermissionsPluginPermissions plugin = new PermissionsPluginPermissions(pluginPermissions);
        final Permissions permissions = new Permissions(ImmutableSet.of(restPermissions, plugin));

        assertThat(permissions.allPermissionsMap().get("foo"))
                .containsOnly("bar", "baz");
        assertThat(permissions.allPermissionsMap().get("hello"))
                .containsOnly("world");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPluginPermissionsWithDuplicatePermission() throws Exception {
        final ImmutableSet<Permission> pluginPermissions = ImmutableSet.of(
                Permission.create("users:edit", "User edit")
        );
        final PermissionsPluginPermissions plugin = new PermissionsPluginPermissions(pluginPermissions);

        new Permissions(ImmutableSet.of(restPermissions, plugin));
    }

    @Test
    public void testUserSelfEditPermissions() throws Exception {
        assertThat(permissions.userSelfEditPermissions("john"))
                .containsExactly("users:edit:john", "users:passwordchange:john", "users:tokenlist:john",
                        "users:tokencreate:john", "users:tokenremove:john");
    }

    @Test
    public void testReaderBasePermissionsForUser() throws Exception {
        final HashSet<String> readerPermissions = new HashSet<>();

        readerPermissions.addAll(permissions.readerBasePermissions());
        readerPermissions.add("users:edit:john");
        readerPermissions.add("users:passwordchange:john");
        readerPermissions.add("users:tokenlist:john");
        readerPermissions.add("users:tokencreate:john");
        readerPermissions.add("users:tokenremove:john");

        assertThat(permissions.readerPermissions("john"))
                .containsOnlyElementsOf(readerPermissions);
    }

    @Test
    public void testAllPermissions() throws Exception {
        assertThat(permissions.allPermissions())
                .containsOnlyElementsOf(restPermissions.permissions()
                        .stream()
                        .map(Permission::permission)
                        .collect(Collectors.toSet()));
    }
}
