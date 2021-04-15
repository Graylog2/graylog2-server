package org.graylog2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class IgnoreDeceptiveExceptionExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if(context.getTestMethod().isPresent()
                && context.getTestMethod().get().isAnnotationPresent(IgnoreDeceptiveExceptionsAnnotation.class)) {
            final String className = context.getTestMethod().get().getAnnotation(IgnoreDeceptiveExceptionsAnnotation.class).clazz().getName();
            Configurator.setLevel(className, Level.OFF);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if(context.getTestMethod().isPresent()
                && context.getTestMethod().get().isAnnotationPresent(IgnoreDeceptiveExceptionsAnnotation.class)) {
            final String className = context.getTestMethod().get().getAnnotation(IgnoreDeceptiveExceptionsAnnotation.class).clazz().getName();
            Configurator.setLevel(className, Level.INFO);
        }
    }
}
