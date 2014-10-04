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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.reflections.Reflections;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ResourceRoutesParser {

    private final String sourcePackage;

    public ResourceRoutesParser(String sourcePackage) {
        this.sourcePackage = sourcePackage;
    }

    private Set<Class<?>> httpMethods() {
        Set<Class<?>> methods = Sets.newHashSet();
        methods.add(GET.class);
        methods.add(POST.class);
        methods.add(PUT.class);
        methods.add(DELETE.class);

        return methods;
    }

    private Class<?> httpMethodOfMethod(Method method) {
        for (Class methodAnn : httpMethods()) {
            if (method.getAnnotation(methodAnn) != null)
                return methodAnn;
        }

        return null;
    }


    public List<RouteClass> buildClasses() {
        Reflections ref = new Reflections(sourcePackage);
        Set<Class<?>> classes = ref.getTypesAnnotatedWith(Path.class);

        List<RouteClass> routeClassList = Lists.newArrayList();

        for (Class<?> klazz : classes) {
            RouteClass routeClass = buildRouteClass(klazz);
            if (routeClass == null) continue;

            routeClassList.add(routeClass);
        }

        return routeClassList;
    }

    private RouteClass buildRouteClass(Class<?> klazz) {
        Path pathAnn = klazz.getAnnotation(Path.class);
        if (pathAnn == null)
            return null;

        String path = pathAnn.value();

        RouteClass routeClass = new RouteClass(klazz, path);

        for (Method method : klazz.getMethods()) {
            Route route = buildRouteForMethod(klazz, path, method);
            if (route == null) continue;
            routeClass.addRoute(route);
        }
        return routeClass;
    }

    private Route buildRouteForMethod(Class<?> klazz, String pathPrefix, Method method) {
        Set<Annotation> anns = Sets.newHashSet(method.getAnnotations());
        Class<?> httpMethod = httpMethodOfMethod(method);

        if (httpMethod == null)
            return null;

        Path ann = method.getAnnotation(Path.class);

        String absolutePath = getAbsolutePath(pathPrefix, ann);

        //System.out.println(httpMethod.getSimpleName() + "\t\t" + buildPath + ": " + klazz.getSimpleName() + "." + method.getName());
        Map<PathParam, Class<?>> pathParamTypeMap = getPathParamMap(method);

        return new Route(httpMethod.getSimpleName(), absolutePath, klazz, method, pathParamTypeMap);
    }

    private Map<PathParam, Class<?>> getPathParamMap(Method method) {
        Map<PathParam, Class<?>> pathParamTypeMap = Maps.newLinkedHashMap(); // Using a linked hash map due to its preservation insertion order
        int i = 0;
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            for (Annotation annotation :annotations)
                if (annotation instanceof PathParam) {
                    final Class<?> type = method.getParameterTypes()[i];
                    pathParamTypeMap.put((PathParam) annotation, type);
                }
            i++;
        }
        return pathParamTypeMap;
    }

    private String getAbsolutePath(String pathPrefix, Path ann) {
        StringBuilder buildPath = new StringBuilder(pathPrefix);

        if (ann != null) {
            String methodPath = ann.value();
            if (!pathPrefix.endsWith("/") && !methodPath.startsWith("/"))
                buildPath.append("/");

            buildPath.append(methodPath);
        }
        return buildPath.toString();
    }
}
