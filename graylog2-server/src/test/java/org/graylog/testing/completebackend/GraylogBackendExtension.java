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

import com.google.common.base.Stopwatch;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;


public class GraylogBackendExtension implements AfterEachCallback, BeforeAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);

    private GraylogBackend backend;
    private Lifecycle lifecycle;

    @Override
    public void beforeAll(ExtensionContext context) {
        Stopwatch sw = Stopwatch.createStarted();

        lifecycle = Lifecycle.from(context);

        backend = GraylogBackend.createStarted();

        context.getStore(NAMESPACE).put(context.getRequiredTestClass().getName(), backend);

        sw.stop();

        LOG.info("Backend started after " + sw.elapsed(TimeUnit.SECONDS) + " seconds");
    }

    @Override
    public void afterEach(ExtensionContext context) {
        lifecycle.afterEach(backend);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(GraylogBackend.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return extensionContext.getStore(NAMESPACE).get(extensionContext.getRequiredTestClass().getName());
    }
}
