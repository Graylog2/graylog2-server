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
package org.graylog.autovalue;

import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.Map;

import static java.util.Objects.requireNonNull;

class Generator {
    private final AutoValueExtension.Context context;
    private final String className;
    private final String classToExtend;
    private final boolean isFinal;

    Generator(AutoValueExtension.Context context, String className, String classToExtend, boolean isFinal) {
        this.context = requireNonNull(context);
        this.className = requireNonNull(className);
        this.classToExtend = requireNonNull(classToExtend);
        this.isFinal = isFinal;
    }

    TypeSpec.Builder classBuilder() {
        return TypeSpec.classBuilder(className)
                .superclass(ClassName.get(context.packageName(), classToExtend))
                .addModifiers(isFinal ? Modifier.FINAL : Modifier.ABSTRACT);
    }

    MethodSpec superConstructor() {
        final MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
        for (Map.Entry<String, ExecutableElement> property : context.properties().entrySet()) {
            constructor.addParameter(TypeName.get(property.getValue().getReturnType()), property.getKey());
        }

        constructor.addStatement("super($L)", Joiner.on(", ").join(context.properties().keySet()));

        return constructor.build();
    }
}
