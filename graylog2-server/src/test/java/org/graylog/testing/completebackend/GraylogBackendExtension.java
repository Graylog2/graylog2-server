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
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;


public class GraylogBackendExtension implements AfterEachCallback, BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackendExtension.class);
    private static final Namespace NAMESPACE = Namespace.create(GraylogBackendExtension.class);


    private ElasticsearchInstance es;
    private MongoDBInstance mongodb;
    private NodeInstance node;


    @Override
    public void beforeAll(ExtensionContext context) {

        Stopwatch sw = Stopwatch.createStarted();

        startContainers();

        GraylogBackend backend = new GraylogBackend();
        backend.port = node.getPort();
        backend.address = node.getApiAddress();

        context.getStore(NAMESPACE).put(context.getRequiredTestClass().getName(), backend);

        sw.stop();

        LOG.info("Containers started after " + sw.elapsed(TimeUnit.SECONDS) + " seconds");
    }

    // Assuming that parallel start works, because
    // - mongodb and es are independent
    // - node will retry connections to mongodb and es until they are there
    public void startContainers() {
        Network network = Network.newNetwork();

        ExecutorService executor = Executors.newFixedThreadPool(3);

        FutureTask<ElasticsearchInstance> esTask = runTask(executor, () -> ElasticsearchInstance.create(network));
        FutureTask<MongoDBInstance> mongodbTask = runTask(executor, () -> MongoDBInstance.createStarted(network, MongoDBInstance.Lifecycle.CLASS));
        FutureTask<NodeInstance> nodeTask = runTask(executor, () -> NodeInstance.createStarted(network, MongoDBInstance.internalUri(), ElasticsearchInstance.internalUri()));

        try {
            es = esTask.get();
            mongodb = mongodbTask.get();
            node = nodeTask.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Container creation aborted", e);
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    public <T> FutureTask<T> runTask(ExecutorService executor, Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        executor.execute(task);
        return task;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        LOG.warn("after");
        node.stop();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        mongodb.dropDatabase();
        es.cleanUp();
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
