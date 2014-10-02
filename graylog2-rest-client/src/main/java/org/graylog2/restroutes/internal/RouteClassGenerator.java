/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
