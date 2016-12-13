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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.extension.AutoValueExtension;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.Map;
import java.util.Set;

import static com.gabrielittner.auto.value.util.AutoValueUtil.newTypeSpecBuilder;

/**
 * Creates Java Bean getter methods for each property.
 */
public class WithBeanGetterExtension extends AutoValueExtension {
    private static final Set<TypeName> SKIP_ANNOTATIONS = ImmutableSet.of(
            TypeName.get(JsonIgnore.class),
            TypeName.get(JsonProperty.class),
            TypeName.get(Override.class)
    );

    @Override
    public boolean applicable(Context context) {
        return context.autoValueClass().getAnnotation(WithBeanGetter.class) != null;
    }

    @Override
    public String generateClass(Context context, String className, String classToExtend, boolean isFinal) {
        final TypeSpec.Builder typeSpecBuilder = newTypeSpecBuilder(context, className, classToExtend, isFinal);
        final Map<String, ExecutableElement> properties = context.properties();
        for (Map.Entry<String, ExecutableElement> entry : properties.entrySet()) {
            typeSpecBuilder.addMethod(generateGetterMethod(entry.getKey(), entry.getValue()));
        }

        final JavaFile javaFile = JavaFile.builder(context.packageName(), typeSpecBuilder.build()).build();

        return javaFile.toString();
    }

    private MethodSpec generateGetterMethod(String name, ExecutableElement element) {
        final TypeName returnType = ClassName.get(element.getReturnType());
        final String prefix = isBoolean(returnType) ? "is" : "get";
        final String getterName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
        final MethodSpec.Builder builder = MethodSpec.methodBuilder(prefix + getterName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addAnnotation(AnnotationSpec.builder(JsonIgnore.class).build())
                .addStatement("return $N()", name)
                .returns(returnType);

        // Copy all annotations but @JsonProperty, @JsonIgnore, and @Override to the new method.
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            final TypeName annotationType = ClassName.get(annotationMirror.getAnnotationType());
            if (SKIP_ANNOTATIONS.contains(annotationType)) {
                continue;
            }
            builder.addAnnotation(AnnotationSpec.get(annotationMirror));
        }

        return builder.build();
    }

    private boolean isBoolean(TypeName typeName) {
        return typeName.equals(ClassName.BOOLEAN) || typeName.equals(ClassName.BOOLEAN.box());
    }
}
