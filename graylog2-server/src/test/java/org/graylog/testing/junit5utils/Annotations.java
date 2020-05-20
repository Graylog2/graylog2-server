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
