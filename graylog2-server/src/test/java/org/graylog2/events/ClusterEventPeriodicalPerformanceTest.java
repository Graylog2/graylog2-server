package org.graylog2.events;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class ClusterEventPeriodicalPerformanceTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventPeriodicalPerformanceTest.class);
    private static final int CONSUMER_COUNT = 10;
    private static final int PRODUCER_COUNT = 5;
    private static final int EVENTS_PER_PRODUCER = 1000;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private MongoDBTestService mongodb;
    private MongoJackObjectMapperProvider objectMapperProvider;

    @BeforeEach
    public void setUp(MongoDBTestService mongodb,
                      MongoJackObjectMapperProvider objectMapperProvider) {
        this.mongodb = mongodb;
        this.objectMapperProvider = objectMapperProvider;
    }

    class EventSubscriber {
        private final AtomicInteger counter = new AtomicInteger(PRODUCER_COUNT * EVENTS_PER_PRODUCER);
        private final Runnable callback;

        public EventSubscriber(Runnable callback) {
            this.callback = callback;
        }

        @Subscribe
        public void countEvents(DummyEvent event) {
            final var c = counter.decrementAndGet();
            if (c <= 0) {
                callback.run();
            }
        }
    }

    @Test
    void concurrentAccessDoesNotLeadToWriteAmplification() throws InterruptedException {
        final var producerCountdown = new CountDownLatch(PRODUCER_COUNT);
        final var consumerCountdown = new CountDownLatch(CONSUMER_COUNT);
        final var producerStopwatch = Stopwatch.createStarted();
        final var consumerStopwatch = Stopwatch.createStarted();

        final var totalCount = 2 * (2 * CONSUMER_COUNT + 2 * PRODUCER_COUNT);
        try (final var threadPool = Executors.newFixedThreadPool(totalCount, r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        })) {
            for (int i = 0; i < CONSUMER_COUNT; i++) {
                final var serverEventBus = new EventBus();
                final var clusterEventBus = new ClusterEventBus();
                final var periodical = new ClusterEventPeriodical(objectMapperProvider, mongodb.mongoConnection(),
                        new SimpleNodeId("consumer-" + i), new RestrictedChainingClassLoader(
                        new ChainingClassLoader(this.getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()), serverEventBus, clusterEventBus);
                threadPool.submit(() -> {
                    while (running.get()) {
                        periodical.run();
                        try {
                            Thread.sleep(Duration.ofSeconds(periodical.getPeriodSeconds()));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

                serverEventBus.register(new EventSubscriber(consumerCountdown::countDown));
            }

            for (int i = 0; i < PRODUCER_COUNT; i++) {
                final var serverEventBus = new EventBus();
                final var clusterEventBus = new ClusterEventBus();
                final var nodeId = new SimpleNodeId("producer-" + i);
                final var periodical = new ClusterEventPeriodical(objectMapperProvider, mongodb.mongoConnection(),
                        nodeId, new RestrictedChainingClassLoader(
                        new ChainingClassLoader(this.getClass().getClassLoader()),
                        SafeClasses.allGraylogInternal()), serverEventBus, clusterEventBus);

                threadPool.submit(periodical);

                threadPool.submit(() -> {
                    for (int count = 0; count < EVENTS_PER_PRODUCER; count++) {
                        clusterEventBus.post(new DummyEvent(nodeId + "-" + count));
                    }
                    producerCountdown.countDown();
                });
            }

            producerCountdown.await(5, TimeUnit.MINUTES);
            LOG.info("Producers have finished, took: " + producerStopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms.");

            consumerCountdown.await(10, TimeUnit.MINUTES);
            LOG.info("Consumers have finished, took: " + consumerStopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms.");

            running.set(false);

            printStatistics(mongodb.mongoDatabase());
        }
    }

    private void printStatistics(MongoDatabase database) {
        final var stats = database.runCommand(new Document("serverStatus", 1));
        final var ops = stats.get("opcounters", Document.class);
        final var reads = ops.getLong("query");
        final var writes = Stream.of("insert", "update", "delete").map(ops::getLong).reduce(Long::sum).orElseThrow();

        LOG.info("Reads = " + reads + ", Writes = " + writes);
    }
}
