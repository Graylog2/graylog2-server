package org.graylog2.restroutes;

import com.sun.codemodel.*;
import org.graylog2.restroutes.internal.ResourceRoutesParser;
import org.graylog2.restroutes.internal.RouteClassGenerator;
import org.graylog2.restroutes.internal.RouteClass;
import org.graylog2.restroutes.internal.RouterGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GenerateRoutes {
    private static final String packagePrefix = "org.graylog2.restroutes.generated";

    public static void main(String[] argv) {
        // Just "touching" class in server jar so it gets loaded.
        org.graylog2.rest.resources.RestResource resource = null;
        org.graylog2.radio.rest.resources.RestResource radioResource = null;

        JCodeModel codeModel = new JCodeModel();

        JDefinedClass router = null;
        try {
            router = codeModel._class(packagePrefix + ".routes");
        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        final ResourceRoutesParser parser = new ResourceRoutesParser("org.graylog2.rest.resources");

        final List<RouteClass> routeClassList = parser.buildClasses();

        final RouteClassGenerator generator = new RouteClassGenerator(packagePrefix, codeModel);

        final RouterGenerator routerGenerator = new RouterGenerator(router, generator);
        routerGenerator.build(routeClassList);

        // do the same for radio resources
        JDefinedClass radioRouter = null;
        try {
            radioRouter = codeModel._class(packagePrefix + ".Radio");
        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        final ResourceRoutesParser radioParser = new ResourceRoutesParser("org.graylog2.radio.rest.resources");
        final List<RouteClass> radioRouteClassList = radioParser.buildClasses();
        final RouteClassGenerator radioGenerator = new RouteClassGenerator(packagePrefix + ".radio", codeModel);
        final RouterGenerator radioRouterGenerator = new RouterGenerator(radioRouter, radioGenerator, JMod.PUBLIC);
        radioRouterGenerator.build(radioRouteClassList);

        JMethod radioMethod = router.method(JMod.PUBLIC | JMod.STATIC, radioRouter, "radio");
        radioMethod.body().directStatement("return new " + radioRouter.name() + "();");

        try {
            File dest = new File(argv[0]);
            codeModel.build(dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
