package org.graylog2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.graylog2.blacklists.BlacklistCache;
import org.graylog2.database.MongoBridge;
import org.graylog2.database.MongoConnection;
import org.graylog2.forwarders.forwarders.LogglyForwarder;
import org.graylog2.indexer.Indexer;
import org.graylog2.indexer.EmbeddedElasticSearchClient;
import org.graylog2.messagehandlers.amqp.AMQPBroker;
import org.graylog2.messagehandlers.amqp.AMQPSubscribedQueue;
import org.graylog2.messagehandlers.amqp.AMQPSubscriberThread;
import org.graylog2.messagehandlers.gelf.ChunkedGELFClientManager;
import org.graylog2.messagehandlers.gelf.GELFMainThread;
import org.graylog2.messagehandlers.syslog.SyslogServerThread;
import org.graylog2.messagequeue.MessageQueue;
import org.graylog2.messagequeue.MessageQueueFlusher;
import org.graylog2.periodical.BulkIndexerThread;
import org.graylog2.periodical.ChunkedGELFClientManagerThread;
import org.graylog2.periodical.HostCounterCacheWriterThread;
import org.graylog2.periodical.MessageCountWriterThread;
import org.graylog2.periodical.MessageRetentionThread;
import org.graylog2.periodical.ServerValueWriterThread;
import org.graylog2.streams.StreamCache;

public class GraylogServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(GraylogServer.class);

    private final MongoConnection mongoConnection;
    private final MongoBridge mongoBridge;
    private final Configuration configuration;
    private RulesEngine drools;

    private static final int SCHEDULED_THREADS_POOL_SIZE = 7;
    private ScheduledExecutorService scheduler;

    static final String GRAYLOG2_VERSION = "0.9.7-dev";

    private final Indexer indexer;

    private final ServerValue serverValue;

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

        serverValue = new ServerValue(this);
    }

    @Override
    public void run() {

        // initiate the mongodb connection, this might fail but it will retry to establish the connection
        mongoConnection.connect();
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

        initializeRulesEngine(configuration.getDroolsRulesFile());
        initializeSyslogServer(configuration.getSyslogProtocol(), configuration.getSyslogListenPort(), configuration.getSyslogListenAddress());
        initializeHostCounterCache(scheduler);

        // Start message counter thread.
        initializeMessageCounters(scheduler);

        // Inizialize message queue.
        initializeMessageQueue(scheduler, configuration);

        // Write initial ServerValue information.
        writeInitialServerValues(configuration);

        // Start GELF threads
        if (configuration.isUseGELF()) {
            initializeGELFThreads(configuration.getGelfListenAddress(), configuration.getGelfListenPort(), scheduler);
        }

        // Initialize AMQP Broker if enabled
        if (configuration.isAmqpEnabled()) {
            initializeAMQP(configuration);
        }

        // Start server value writer thread. (writes for example msg throughout and pings)
        initializeServerValueWriter(scheduler);

        // Start thread that automatically removes messages older than retention time.
        if (configuration.performRetention()) {
            initializeMessageRetentionThread(scheduler);
        } else {
            LOG.info("Not initializing retention time cleanup thread because --no-retention was passed.");
        }

        // Add a shutdown hook that tries to flush the message queue.
        Runtime.getRuntime().addShutdownHook(new MessageQueueFlusher(this));

        LOG.info("Graylog2 up and running.");

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                /* ignore */
            }
        }

    }

    private void initializeHostCounterCache(ScheduledExecutorService scheduler) {

        scheduler.scheduleAtFixedRate(new HostCounterCacheWriterThread(this), HostCounterCacheWriterThread.INITIAL_DELAY, HostCounterCacheWriterThread.PERIOD, TimeUnit.SECONDS);

        LOG.info("Host count cache is up.");
    }

    private void initializeMessageQueue(ScheduledExecutorService scheduler, Configuration configuration) {
        // Set the maximum size if it was configured to something else than 0 (= UNLIMITED)
        if (configuration.getMessageQueueMaximumSize() != MessageQueue.SIZE_LIMIT_UNLIMITED) {
            MessageQueue.getInstance().setMaximumSize(configuration.getMessageQueueMaximumSize());
        }

        scheduler.scheduleAtFixedRate(new BulkIndexerThread(this, configuration), BulkIndexerThread.INITIAL_DELAY, configuration.getMessageQueuePollFrequency(), TimeUnit.SECONDS);

        LOG.info("Message queue initialized .");
    }

    private void initializeMessageCounters(ScheduledExecutorService scheduler) {

        scheduler.scheduleAtFixedRate(new MessageCountWriterThread(this), MessageCountWriterThread.INITIAL_DELAY, MessageCountWriterThread.PERIOD, TimeUnit.SECONDS);

        LOG.info("Message counters initialized.");
    }

    private void initializeServerValueWriter(ScheduledExecutorService scheduler) {

        scheduler.scheduleAtFixedRate(new ServerValueWriterThread(this), ServerValueWriterThread.INITIAL_DELAY, ServerValueWriterThread.PERIOD, TimeUnit.SECONDS);

        LOG.info("Server value writer up.");
    }

    private void initializeMessageRetentionThread(ScheduledExecutorService scheduler) {
        // Schedule first run. This is NOT at fixed rate. Thread will itself schedule next run with current frequency setting from database.
        scheduler.schedule(new MessageRetentionThread(this),0,TimeUnit.SECONDS);

        LOG.info("Retention time management active.");
    }

    private void initializeGELFThreads(String gelfAddress, int gelfPort, ScheduledExecutorService scheduler) {
        GELFMainThread gelfThread = new GELFMainThread(this, new InetSocketAddress(gelfAddress, gelfPort));
        gelfThread.start();

        scheduler.scheduleAtFixedRate(new ChunkedGELFClientManagerThread(ChunkedGELFClientManager.getInstance()), ChunkedGELFClientManagerThread.INITIAL_DELAY, ChunkedGELFClientManagerThread.PERIOD, TimeUnit.SECONDS);

        LOG.info("GELF threads started");
    }

    private void initializeSyslogServer(String syslogProtocol, int syslogPort, String syslogHost) {

        // Start the Syslog thread that accepts syslog packages.
        SyslogServerThread syslogServerThread = new SyslogServerThread(this, syslogProtocol, syslogPort, syslogHost);
        syslogServerThread.start();

        // Check if the thread started up completely.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        if (syslogServerThread.getCoreThread().isAlive()) {
            LOG.info("Syslog server thread is up.");
        } else {
            LOG.fatal("Could not start syslog server core thread. Do you have permissions to listen on port " + syslogPort + "?");
            System.exit(1);
        }
    }

    private void initializeRulesEngine(String rulesFilePath) {
        try {
            if (rulesFilePath != null && !rulesFilePath.isEmpty()) {
                drools = new RulesEngine();
                drools.addRules(rulesFilePath);
                LOG.info("Using rules: " + rulesFilePath);
            } else {
                LOG.info("Not using rules");
            }
        } catch (Exception e) {
            LOG.fatal("Could not load rules engine: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private void initializeAMQP(Configuration configuration) {

        // Connect to AMQP broker.
        AMQPBroker amqpBroker = new AMQPBroker(
                configuration.getAmqpHost(),
                configuration.getAmqpPort(),
                configuration.getAmqpUsername(),
                configuration.getAmqpPassword(),
                configuration.getAmqpVirtualhost()
        );

        List<AMQPSubscribedQueue> amqpQueues = configuration.getAmqpSubscribedQueues();

        if (amqpQueues != null) {
            // Start AMQP subscriber thread for each queue to listen on.
            for (AMQPSubscribedQueue queue : amqpQueues) {
                AMQPSubscriberThread amqpThread = new AMQPSubscriberThread(this, queue, amqpBroker);
                amqpThread.start();
            }

            LOG.info("AMQP threads started. (" + amqpQueues.size() + " queues)");
        }
    }

    public void writeInitialServerValues(Configuration configuration) {
        serverValue.setStartupTime(Tools.getUTCTimestamp());
        serverValue.setPID(Integer.parseInt(Tools.getPID()));
        serverValue.setJREInfo(Tools.getSystemInformation());
        serverValue.setGraylog2Version(GRAYLOG2_VERSION);
        serverValue.setAvailableProcessors(HostSystem.getAvailableProcessors());
        serverValue.setLocalHostname(Tools.getLocalHostname());
        serverValue.writeMessageQueueMaximumSize(configuration.getMessageQueueMaximumSize());
        serverValue.writeMessageQueueBatchSize(configuration.getMessageQueueBatchSize());
        serverValue.writeMessageQueuePollFrequency(configuration.getMessageQueuePollFrequency());
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

    public RulesEngine getDrools() {
        return drools;
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public ServerValue getServerValue() {
        return serverValue;
    }

}
