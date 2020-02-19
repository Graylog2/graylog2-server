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

import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog.testing.graylognode.NodeInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class GraylogBackend {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogBackend.class);
    private final ElasticsearchInstance es;
    private final MongoDBInstance mongodb;
    private final NodeInstance node;

    // Assuming that parallel start works, because
    // - mongodb and es are independent
    // - node will retry connections to mongodb and es until they are there
    public static GraylogBackend createStarted() {
        Network network = Network.newNetwork();

        ExecutorService executor = Executors.newFixedThreadPool(3);

        FutureTask<ElasticsearchInstance> esTask = runTask(executor, () -> ElasticsearchInstance.create(network));
        FutureTask<MongoDBInstance> mongodbTask = runTask(executor, () -> MongoDBInstance.createStarted(network, MongoDBInstance.Lifecycle.CLASS));
        FutureTask<NodeInstance> nodeTask = runTask(executor, () -> NodeInstance.createStarted(network, MongoDBInstance.internalUri(), ElasticsearchInstance.internalUri()));

        try {
            return new GraylogBackend(esTask.get(), mongodbTask.get(), nodeTask.get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Container creation aborted", e);
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    private static <T> FutureTask<T> runTask(ExecutorService executor, Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        executor.execute(task);
        return task;
    }

    private GraylogBackend(ElasticsearchInstance es, MongoDBInstance mongodb, NodeInstance node) {
        this.es = es;
        this.mongodb = mongodb;
        this.node = node;
    }

    public void shutDown() {
        node.stop();
    }

    public void purgeData() {
        mongodb.dropDatabase();
        es.cleanUp();
    }

    public String apiAddress() {
        return node.getApiAddress();
    }
}
