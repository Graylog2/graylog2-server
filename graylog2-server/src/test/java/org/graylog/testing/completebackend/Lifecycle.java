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
package org.graylog.testing.completebackend;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Optional;

/**
 * Controls the lifecycle of the {@link GraylogBackend} used in tests
 */
public enum Lifecycle {
    /**
     * {@link GraylogBackend} will be reused for all tests in a class. Use this, if you can make sure
     * that the individual tests will not interfere with each other, e.g., by creating test data that
     * would affect the outcome of a different test.
     */
    CLASS,
    /**
     * A fresh {@link GraylogBackend} will be instantiated for each tests in a class. This is the safest
     * way to isolate tests. Test execution will take much longer due to the time it takes to spin up
     * the necessary container, especially the server node itself.
     */
    METHOD {
        @Override
        void afterEach(GraylogBackend backend) {
            backend.fullReset();
        }
    };

    void afterEach(GraylogBackend backend) {
    }

    public static Lifecycle from(ExtensionContext context) {
        Optional<Class<?>> testClass = context.getTestClass();

        if (!testClass.isPresent())
            throw new RuntimeException("Error determining test class from ExtensionContext");

        return testClass.get().getAnnotation(ApiIntegrationTest.class).serverLifecycle();
    }
}
