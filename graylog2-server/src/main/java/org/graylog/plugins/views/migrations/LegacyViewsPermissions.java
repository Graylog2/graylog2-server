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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

class LegacyViewsPermissions {
    static final String VIEW_USE = "view:use";
    static final String VIEW_CREATE = "view:create";
    static final String EXTENDEDSEARCH_CREATE = "extendedsearch:create";
    static final String EXTENDEDSEARCH_USE = "extendedsearch:use";

    static Set<String> all() {
        return ImmutableSet.of(
                VIEW_USE,
                VIEW_CREATE,
                EXTENDEDSEARCH_USE,
                EXTENDEDSEARCH_CREATE);
    }
}
