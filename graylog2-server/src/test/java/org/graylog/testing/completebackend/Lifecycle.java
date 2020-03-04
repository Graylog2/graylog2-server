package org.graylog.testing.completebackend;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

public enum Lifecycle {
    CLASS {
        @Override
        void afterEach(GraylogBackend backend) {
            backend.purgeData();
        }
    },
    METHOD {
        @Override
        void afterEach(GraylogBackend backend) {
            backend.fullReset();
        }
    };

    abstract void afterEach(GraylogBackend backend);

    public static Lifecycle from(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();

        if (!testClass.isPresent())
            throw new RuntimeException("Error determining test class from ExtensionContext");

        return testClass.get().getAnnotation(ApiIntegrationTest.class).serverLifecycle();
    }
}
