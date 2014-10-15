/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import org.apache.log4j.Level;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.graylog2.bindings.providers.EsNodeProvider;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ESTimestampFixup {
    private static final Logger LOG = LoggerFactory.getLogger(ESTimestampFixup.class);

    private static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    private static final String PROPERTIES_PREFIX = "graylog2.";

    @Parameters(commandDescription = "Graylog2 ES fixup tool")
    public static class CommandLineOptions {
        @Parameter(names = {"-F", "--fix"}, description = "Fix problems")
        private boolean fix = false;

        @Parameter(names = {"-f", "--configfile"}, description = "Configuration file for Graylog2")
        private String configFile = "/etc/graylog2.conf";

        @Parameter(names = {"-i", "--indices"}, description = "Indices to process (required)", variableArity = true)
        private List<String> indices = Lists.newArrayList();

        @Parameter(names = {"-h", "--help"}, description = "Show usage")
        private boolean help = false;

        @Parameter(names = {"-p", "--port"}, description = "ES TCP transport port")
        private int port = 9351;

        @Parameter(names = {"-b", "--batch"}, description = "ES scroll size (per shard)")
        private int batchSize = 500;

        @Parameter(names = {"-t", "--timeout"}, description = "ES scroll timeout in minutes")
        private int scrollTimeout = 1;

        @Parameter(names = {"-d", "--debug"}, description = "Enable debug output")
        private boolean debug = false;

        public boolean isFix() {
            return fix;
        }

        public String getConfigFile() {
            return configFile;
        }

        public boolean isHelp() {
            return help;
        }

        public String[] getIndicesArray() {
            return indices.toArray(new String[indices.size()]);
        }

        public int getPort() {
            return port;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public int getScrollTimeout() {
            return scrollTimeout;
        }

        public boolean isDebug() {
            return debug;
        }
    }

    public static class Bindings extends AbstractModule {
        private final Configuration configuration;

        public Bindings(final Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        protected void configure() {
            bind(Configuration.class).toInstance(configuration);
            bind(Node.class).toProvider(EsNodeProvider.class).in(Scopes.SINGLETON);
        }
    }

    public static void main(final String[] args) {
        final CommandLineOptions commandLineOptions = new CommandLineOptions();
        final JCommander jCommander = new JCommander(commandLineOptions, args);
        jCommander.setProgramName("graylog2-es-fixup");

        if (commandLineOptions.isDebug()) {
            org.apache.log4j.Logger.getLogger(ESTimestampFixup.class).setLevel(Level.DEBUG);
        } else {
            org.apache.log4j.Logger.getLogger(ESTimestampFixup.class).setLevel(Level.INFO);
        }

        if (commandLineOptions.isHelp()) {
            jCommander.usage();
            System.exit(0);
        }

        if (commandLineOptions.getIndicesArray().length == 0) {
            System.out.println("No indices given. Use '-i graylog2_0 graylog2_1 graylog2_2' command line option.");
            jCommander.usage();
            System.exit(1);
        }

        final JadConfig jadConfig = new JadConfig();
        final Configuration configuration = readConfiguration(jadConfig, commandLineOptions);

        final GuiceInstantiationService instantiationService = new GuiceInstantiationService();
        final Injector injector = Guice.createInjector(new Bindings(configuration));

        instantiationService.setInjector(injector);

        injector.getInstance(ESTimestampFixup.class).run(commandLineOptions, args);
    }

    private final Node node;
    private final Configuration configuration;

    @Inject
    public ESTimestampFixup(final Node node, final Configuration configuration, final CommandLineOptions options) {
        this.node = node;
        this.configuration = configuration;
    }

    private void run(final CommandLineOptions commandLineOptions, final String[] args) {
        startEsNode();

        final Client client = node.client();
        final SearchRequestBuilder srb = client.prepareSearch();

        final CountResponse countResponse = client.prepareCount(commandLineOptions.getIndicesArray()).execute().actionGet();
        final long totalCount = countResponse.getCount();

        long changedCount = 0;
        long processedCount = 0;

        srb.setIndices(commandLineOptions.getIndicesArray());
        srb.setSearchType(SearchType.SCAN);
        srb.setScroll(TimeValue.timeValueMinutes(commandLineOptions.getScrollTimeout()));
        srb.setQuery(QueryBuilders.matchAllQuery());
        srb.setSize(commandLineOptions.getBatchSize());
        srb.addField("_id");
        srb.addField("timestamp");
        srb.addField("_source");

        final SearchRequest request = srb.request();
        final SearchResponse response = client.search(request).actionGet();

        if (! commandLineOptions.isFix()) {
            LOG.warn("Not executing update because '-F' command line flag not given!");
        }

        while (true) {
            final SearchResponse r = client.prepareSearchScroll(response.getScrollId()).setScroll(TimeValue.timeValueMinutes(1)).execute().actionGet();

            if (r.getHits().getHits().length == 0) {
                LOG.debug("No more hits, done!");
                break;
            }

            final BulkRequestBuilder bulk = client.prepareBulk();

            for (SearchHit hit : r.getHits()) {
                processedCount++;
                try {
                    if (handleHit(hit, bulk)) {
                        changedCount++;
                    }
                } catch (Exception e) {
                    LOG.error("Error handling document " + hit.getId(), e);
                }
            }

            processBulk(bulk, commandLineOptions.isFix());
            LOG.info("Changed {} of total {} documents ({}% checked)", changedCount, totalCount, String.format("%.2f", ((double) processedCount / totalCount) * 100));
        }

        stopEsNode();
    }

    private boolean handleHit(SearchHit hit, BulkRequestBuilder bulk) {
        if (hit.field("timestamp").value() instanceof Long) {
            LOG.debug("UPDATE {}/{}/{} (from {})", hit.getIndex(), hit.getType(), hit.getId(), hit.field("timestamp").value().getClass());

            final Map<String, Object> source = hit.getSource();
            final long timestamp = hit.field("timestamp").getValue();

            // Convert the timestamp into the correct format. (long -> ISO8601 string)
            // Make sure there is a _id field with the ES document id.
            // https://github.com/Graylog2/graylog2-server/issues/728
            // https://github.com/Graylog2/graylog2-server/commit/18c471e7117472baa07d42011e0a11b48ff1b625
            source.put("timestamp", Tools.buildElasticSearchTimeFormat(new DateTime(timestamp, DateTimeZone.UTC)));
            if (hit.field("_id") == null) {
                source.put("_id", hit.getId());
            }

            bulk.add(
                    node.client()
                            .prepareIndex(hit.getIndex(), hit.getType(), hit.getId())
                            .setSource(source)
            );

            return true;
        }

        return false;
    }

    private void startEsNode() {
        LOG.debug("Starting ES node (port={})", configuration.getEsTransportTcpPort());
        node.start();
        node.client().admin().cluster().health(new ClusterHealthRequest().waitForYellowStatus())
                .actionGet(configuration.getEsClusterDiscoveryTimeout(), TimeUnit.MILLISECONDS);
    }

    private void stopEsNode() {
        LOG.debug("Stopping ES node");
        node.stop();
    }

    private void processBulk(BulkRequestBuilder bulk, boolean fix) {
        if (fix) {
            if (bulk.numberOfActions() > 0) {
                LOG.info("Executing {} bulk actions", bulk.numberOfActions());
                final BulkResponse bulkResponse = bulk.execute().actionGet();

                if (bulkResponse.hasFailures()) {
                    LOG.error("BULK ERROR {}", bulkResponse.buildFailureMessage());
                } else {
                    LOG.info("Bulk action took {}ms", bulkResponse.getTookInMillis());
                }
            } else {
                LOG.debug("No bulk actions to execute!");
            }
        }
    }

    private static Configuration readConfiguration(final JadConfig jadConfig, final CommandLineOptions options) {
        final Configuration configuration = new Configuration();
        final Map<String, String> config = ImmutableMap.of("elasticsearch_transport_tcp_port", String.valueOf(options.getPort()));

        jadConfig.addConfigurationBean(configuration);
        jadConfig.setRepositories(Arrays.asList(
                new InMemoryRepository(config),
                new EnvironmentRepository(ENVIRONMENT_PREFIX),
                new SystemPropertiesRepository(PROPERTIES_PREFIX),
                new PropertiesRepository(options.getConfigFile())
        ));

        LOG.debug("Loading configuration from config file: {}", options.getConfigFile());
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        return configuration;
    }
}
