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
package org.graylog2.shared.security;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.RestPermission;
import org.graylog2.plugin.security.RestPermissionsPlugin;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RestPermissionsTest {

    private RestPermissions permissions;

    private static class PermissionsPlugin implements RestPermissionsPlugin {
        private final Set<RestPermission> permission;

        public PermissionsPlugin(Set<RestPermission> permissions) {
            this.permission = permissions;
        }

        @Override
        public Set<RestPermission> permissions() {
            return permission;
        }
    }


    @Before
    public void setUp() throws Exception {
        this.permissions = new RestPermissions();
    }

    @Test
    public void testPluginPermissions() throws Exception {
        final ImmutableSet<RestPermission> pluginPermissions = ImmutableSet.of(
                RestPermission.builder().value("foo:bar").description("bar").build(),
                RestPermission.builder().value("foo:baz").description("baz").build(),
                RestPermission.builder().value("hello:world").description("hello").build()
        );
        final PermissionsPlugin plugin = new PermissionsPlugin(pluginPermissions);
        final RestPermissions permissions = new RestPermissions(ImmutableSet.of(plugin));

        assertThat(permissions.allPermissions().get("foo"))
                .containsExactly("bar", "baz");
        assertThat(permissions.allPermissions().get("hello"))
                .containsExactly("world");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPluginPermissionsWithDuplicatePermission() throws Exception {
        final ImmutableSet<RestPermission> pluginPermissions = ImmutableSet.of(
                RestPermission.builder().value("users:edit").description("User edit").build()
        );
        final PermissionsPlugin plugin = new PermissionsPlugin(pluginPermissions);

        new RestPermissions(ImmutableSet.of(plugin));
    }

    @Test
    public void testUserSelfEditPermissions() throws Exception {
        assertThat(permissions.userSelfEditPermissions("john"))
                .containsExactly("users:edit:john", "users:passwordchange:john");
    }

    @Test
    public void testReaderBasePermissionsForUser() throws Exception {
        final HashSet<String> readerPermissions = new HashSet<>();

        readerPermissions.addAll(permissions.readerBasePermissions());
        readerPermissions.add("users:edit:john");
        readerPermissions.add("users:passwordchange:john");

        assertThat(permissions.readerPermissions("john"))
                .containsOnlyElementsOf(readerPermissions);
    }
}
