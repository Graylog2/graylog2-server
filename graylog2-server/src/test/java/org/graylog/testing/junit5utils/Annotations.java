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
package org.graylog.testing.junit5utils;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;

public class Annotations {
    /**
     * @return annotation from test method, if present, or from class otherwise
     * @throws RuntimeException, if annotation wasn't found on either class or method
     */
    public static <T extends Annotation> T annotationFrom(ExtensionContext context, Class<T> annotationType) {
        T fromMethod = fromMethod(context, annotationType);

        if (fromMethod != null) {
            return fromMethod;
        }

        T fromClass = fromClass(context, annotationType);

        if (fromClass == null) {
            throw new RuntimeException("Failed to find annotation " + annotationType);
        }

        return fromClass;
    }

    private static <T extends Annotation> T fromClass(ExtensionContext context, Class<T> annotationType) {
        return context
                .getTestClass()
                .map(c -> c.getAnnotation(annotationType))
                .orElse(null);
    }

    private static <T extends Annotation> T fromMethod(ExtensionContext context, Class<T> annotationType) {
        return context
                .getTestMethod()
                .map(c -> c.getAnnotation(annotationType))
                .orElse(null);
    }
}
