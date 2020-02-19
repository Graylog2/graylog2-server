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

import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;


public class GraylogBackendExtension implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);


    private MongoDBInstance mongodb;
    private NodeInstance node;


    @Override
    public void beforeAll(ExtensionContext context) {
        LOG.warn("before");

        Network network = Network.SHARED;

        mongodb = MongoDBInstance.createWithDefaults(network, MongoDBInstance.Lifecycle.CLASS);
        mongodb.startContainer();

        node = new NodeInstance(mongodb.internalUri(), network);

        node.start();

        GraylogBackend backend = new GraylogBackend();
        backend.port = node.getPort();
        backend.address = node.getApiAddress();

        context.getStore(NAMESPACE).put(context.getRequiredTestClass().getName(), backend);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        LOG.warn("after");
        node.stop();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        mongodb.dropDatabase();
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
