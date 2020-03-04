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
