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

import javax.ws.rs.PathParam;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class Route {
    private final String httpMethod;
    private final String path;
    private final Class klazz;
    private final Method method;
    private final Map<PathParam, Class<?>> pathParams;

    public Route(String httpMethod, String path, Class klazz, Method method, Map<PathParam, Class<?>> pathParams) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.klazz = klazz;
        this.method = method;
        this.pathParams = pathParams;
    }

    public Route(String httpMethod, String path, Class klazz, Method method) {
        this(httpMethod, path, klazz, method, new HashMap<PathParam, Class<?>>());
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Class getKlazz() {
        return klazz;
    }

    public Method getMethod() {
        return method;
    }

    public Map<PathParam, Class<?>> getPathParams() {
        return pathParams;
    }
}
