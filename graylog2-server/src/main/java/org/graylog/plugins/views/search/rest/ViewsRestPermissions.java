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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog2.plugin.security.Permission;
import org.graylog2.plugin.security.PluginPermissions;

import java.util.Collections;
import java.util.Set;

import static org.graylog2.plugin.security.Permission.create;

public class ViewsRestPermissions implements PluginPermissions {
    public static final String VIEW_CREATE = "view:create";
    public static final String VIEW_READ = "view:read";
    public static final String VIEW_EDIT = "view:edit";
    public static final String VIEW_DELETE = "view:delete";
    public static final String VIEW_USE = "view:use";
    public static final String DEFAULT_VIEW_SET = "default-view:set";
    public static final String EXTENDEDSEARCH_CREATE = "extendedsearch:create";
    public static final String EXTENDEDSEARCH_USE = "extendedsearch:use";

    private final ImmutableSet<Permission> permissions = ImmutableSet.of(
            create(VIEW_CREATE, "Create new view"),
            create(VIEW_READ, "Read available views"),
            create(VIEW_EDIT, "Edit view"),
            create(VIEW_DELETE, "Delete view"),
            create(VIEW_USE, "Use the views feature"),
            create(DEFAULT_VIEW_SET, "Set default view"),
            create(EXTENDEDSEARCH_CREATE, "Create extended search"),
            create(EXTENDEDSEARCH_USE, "Use the extended search feature")
    );

    @Override
    public Set<Permission> permissions() {
        return permissions;
    }

    @Override
    public Set<Permission> readerBasePermissions() {
        return Collections.emptySet();
    }
}
