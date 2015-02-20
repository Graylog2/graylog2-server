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
package org.graylog2.restroutes;

import com.sun.codemodel.*;
import org.graylog2.restroutes.internal.ResourceRoutesParser;
import org.graylog2.restroutes.internal.RouteClassGenerator;
import org.graylog2.restroutes.internal.RouteClass;
import org.graylog2.restroutes.internal.RouterGenerator;
import org.graylog2.shared.rest.resources.RestResource;

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
        RestResource resource = null;

        JCodeModel codeModel = new JCodeModel();

        JDefinedClass router = null;
        try {
            router = codeModel._class(packagePrefix + ".routes");
        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        final List<RouteClass> sharedRouteClassList = new ResourceRoutesParser("org.graylog2.shared.rest.resources").buildClasses();

        final ResourceRoutesParser parser = new ResourceRoutesParser("org.graylog2.rest.resources");

        final List<RouteClass> routeClassList = parser.buildClasses();
        routeClassList.addAll(sharedRouteClassList);

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
        radioRouteClassList.addAll(sharedRouteClassList);
        final RouteClassGenerator radioGenerator = new RouteClassGenerator(packagePrefix + ".radio", codeModel);
        final RouterGenerator radioRouterGenerator = new RouterGenerator(radioRouter, radioGenerator, JMod.PUBLIC);
        radioRouterGenerator.build(radioRouteClassList);

        JMethod radioMethod = router.method(JMod.PUBLIC | JMod.STATIC, radioRouter, "radio");
        radioMethod.body().directStatement("return new " + radioRouter.name() + "();");

        try {
            File dest = new File(argv[0]);

            if(!dest.exists()) {
                if(!dest.mkdirs()) {
                    throw new RuntimeException("Output directory " + dest + " doesn't exist and couldn't be created.");
                }
            }

            codeModel.build(dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
