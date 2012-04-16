package org.graylog2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;
import org.graylog2.blacklists.BlacklistCache;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;
import org.graylog2.forwarders.forwarders.LogglyForwarder;
import org.graylog2.indexer.EmbeddedElasticSearchClient;
import org.graylog2.indexer.Indexer;
import org.graylog2.initializers.Initializer;
import org.graylog2.inputs.MessageInput;
import org.graylog2.inputs.gelf.GELFChunkManager;
import org.graylog2.messagequeue.MessageQueueFlusher;
import org.graylog2.streams.StreamCache;

public class GraylogServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(GraylogServer.class);

    private final MongoConnection mongoConnection;
    private final MongoBridge mongoBridge;
    private final Configuration configuration;
    private RulesEngine rulesEngine;
    private ServerValue serverValues;
    private GELFChunkManager gelfChunkManager = new GELFChunkManager();

    private static final int SCHEDULED_THREADS_POOL_SIZE = 15;
    private ScheduledExecutorService scheduler;

    public static final String GRAYLOG2_VERSION = "0.9.7-dev";

    private final Indexer indexer;

    private List<Initializer> initializers = new ArrayList<Initializer>();
    private List<MessageInput> inputs = new ArrayList<MessageInput>();

    public GraylogServer(Configuration configuration) {
        this.configuration = configuration; // TODO use dependency injection

        mongoConnection = new MongoConnection();    // TODO use dependency injection
        mongoConnection.setUser(configuration.getMongoUser());
        mongoConnection.setPassword(configuration.getMongoPassword());
        mongoConnection.setHost(configuration.getMongoHost());
        mongoConnection.setPort(configuration.getMongoPort());
        mongoConnection.setDatabase(configuration.getMongoDatabase());
        mongoConnection.setUseAuth(configuration.isMongoUseAuth());
        mongoConnection.setMaxConnections(configuration.getMongoMaxConnections());
        mongoConnection.setThreadsAllowedToBlockMultiplier(configuration.getMongoThreadsAllowedToBlockMultiplier());
        mongoConnection.setReplicaSet(configuration.getMongoReplicaSet());
        mongoConnection.setMessagesCollectionSize(configuration.getMessagesCollectionSize());

        mongoBridge = new MongoBridge();
        mongoBridge.setConnection(mongoConnection); // TODO use dependency injection

        indexer = new EmbeddedElasticSearchClient(this);
        serverValues = new ServerValue(this);
    }

    public void registerInitializer(Initializer initializer) {
        this.initializers.add(initializer);
    }
    
    public void registerInput(MessageInput input) {
        this.inputs.add(input);
    }

    @Override
    public void run() {

        // initiate the mongodb connection, this might fail but it will retry to establish the connection
        mongoConnection.connect();
        gelfChunkManager.start();
        BlacklistCache.initialize(this);
        StreamCache.initialize(this);

        try {
            if (indexer.indexExists()) {
                LOG.info("Index exists. Not creating it.");
            } else {
                LOG.info("Index does not exist! Trying to create it ...");
                if (indexer.createIndex()) {
                    LOG.info("Successfully created index.");
                } else {
                    LOG.fatal("Could not create Index. Terminating.");
                    System.exit(1);
                }
            }
        } catch (IOException e) {
            LOG.fatal("IOException while trying to check Index. Make sure that your ElasticSearch server is running.", e);
            System.exit(1);
        }

        // Statically set timeout for LogglyForwarder.
        // TODO: This is a code smell and needs to be fixed.
        LogglyForwarder.setTimeout(configuration.getForwarderLogglyTimeout());

        scheduler = Executors.newScheduledThreadPool(SCHEDULED_THREADS_POOL_SIZE);

        // Call all registered initializers.
        for (Initializer initializer : this.initializers) {
            initializer.initialize();
            LOG.debug("Initialized: " + initializer.getClass().getSimpleName());
        }
        
        // Call all registered inputs.
        for (MessageInput input : this.inputs) {
            input.initialize(this.configuration, this);
            LOG.debug("Initialized input: " + input.getName());
        }

        // Add a shutdown hook that tries to flush the message queue.
        Runtime.getRuntime().addShutdownHook(new MessageQueueFlusher(this));

        LOG.info("Graylog2 up and running.");

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { /* lol, i don't care */ }
        }

    }

    public MongoConnection getMongoConnection() {
        return mongoConnection;
    }

    public MongoBridge getMongoBridge() {
        return mongoBridge;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setRulesEngine(RulesEngine engine) {
        rulesEngine = engine;
    }

    public RulesEngine getRulesEngine() {
        return rulesEngine;
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public ServerValue getServerValues() {
        return serverValues;
    }

    public GELFChunkManager getGELFChunkManager() {
        return this.gelfChunkManager;
    }

}
