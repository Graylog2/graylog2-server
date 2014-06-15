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
