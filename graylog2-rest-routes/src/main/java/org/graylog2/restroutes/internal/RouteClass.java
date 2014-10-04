/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restroutes.internal;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RouteClass {
    private final Class klazz;
    private final String path;
    private final List<Route> routes;

    public RouteClass(Class klazz, String path) {
        this.klazz = klazz;
        this.path = path;
        this.routes = Lists.newArrayList();
    }

    public Class getKlazz() {
        return klazz;
    }

    public String getPath() {
        return path;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void addRoute(Route route) {
        routes.add(route);
    }
}
