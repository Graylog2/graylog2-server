package org.graylog2.restroutes.internal;

import com.beust.jcommander.internal.Lists;

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
