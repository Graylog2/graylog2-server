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
package org.graylog2.plugin.security;

import com.google.common.collect.ImmutableSet;
import org.graylog.security.authzroles.BuiltinRole;

import java.util.Set;

public interface PluginPermissions {
    Set<Permission> permissions();

    Set<Permission> readerBasePermissions();

    /**
     * A set of built-in roles that should be added to every graylog setup.
     * @return The roles that this plugin provides
     */
    default Set<BuiltinRole> builtinRoles() {
        return ImmutableSet.of();
    }
}
