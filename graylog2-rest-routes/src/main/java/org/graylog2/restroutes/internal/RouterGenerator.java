package org.graylog2.restroutes.internal;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RouterGenerator {
    private final JDefinedClass routerClass;
    private final RouteClassGenerator generator;
    private final int generateMods;

    public RouterGenerator(JDefinedClass routerClass, RouteClassGenerator generator, int generateMods) {
        this.routerClass = routerClass;
        this.generator = generator;
        this.generateMods = generateMods;
    }

    public RouterGenerator(JDefinedClass routerClass, RouteClassGenerator generator) {
        this(routerClass, generator, JMod.PUBLIC | JMod.STATIC);
    }

    public JDefinedClass build(List<RouteClass> routeClassList) {
        for (RouteClass routeClass : routeClassList) {
            JDefinedClass definedClass = generator.generate(routeClass);
            if (definedClass == null) continue;

            addRouterMethod(routerClass, definedClass);
        }

        return routerClass;
    }

    private void addRouterMethod(JDefinedClass router, JDefinedClass definedClass) {
        String className = definedClass.fullName();
        JMethod method = router.method(generateMods, definedClass, definedClass.name());
        JBlock block = method.body();
        block.directStatement("return new " + className + "();");
    }
}
