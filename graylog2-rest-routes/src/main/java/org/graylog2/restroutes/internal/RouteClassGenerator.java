package org.graylog2.restroutes.internal;

import com.sun.codemodel.*;
import org.graylog2.restroutes.PathMethod;

import javax.ws.rs.PathParam;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RouteClassGenerator {
    private final JCodeModel codeModel;
    private final String packagePrefix;

    public RouteClassGenerator(String packagePrefix, JCodeModel codeModel) {
        this.packagePrefix = packagePrefix;
        this.codeModel = codeModel;
    }

    public JDefinedClass generate(RouteClass routeClass) {
        JDefinedClass definedClass;
        try {
            definedClass = codeModel._class(packagePrefix + "." + routeClass.getKlazz().getSimpleName());
        } catch (JClassAlreadyExistsException e) {
            System.out.println("Class " + routeClass.getKlazz().getSimpleName() + " already exists");
            e.printStackTrace();
            return null;
        }

        for (Route route : routeClass.getRoutes()) {
            JMethod method = definedClass.method(JMod.PUBLIC, PathMethod.class, route.getMethod().getName());
            String path = route.getPath();
            for (Map.Entry<PathParam, Class<?>> entry : route.getPathParams().entrySet()) {
                String fieldName = entry.getKey().value();
                method.param(entry.getValue(), fieldName);
                path = path.replace("{" + fieldName + "}", "\"+"+fieldName+"+\"");

            }
            JBlock block = method.body();
            block.directStatement("return new PathMethod(\"" + route.getHttpMethod() + "\", \"" + path + "\");");
        }

        return definedClass;
    }
}
