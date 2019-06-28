package org.graylog2.indexer;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.bindings.providers.JestClientProvider;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.CLUSTER_NAME;
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.TRANSPORT_TCP_PORT;

@RunWith(Parameterized.class)
public class IndexMappingTest {
    private static final String indexName = "testmessages";
    private static final String currentIndex = "testmessages_0";
    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final EmbeddedElastic embeddedElastic;
    private final JestClient jestClient;

    private final IndexSet indexSet;
    private Indices indices;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> provideData() {
        return Arrays.asList(new Object[][]{{"5.3.0"}, {"6.2.0"}});
    }

    public IndexMappingTest(String esVersion) throws IOException, InterruptedException {
        embeddedElastic = EmbeddedElastic.builder()
                .withElasticVersion(esVersion)
                .withSetting(TRANSPORT_TCP_PORT, 0)
                .withSetting(CLUSTER_NAME, "test")
                .withEsJavaOpts("-Xms128m -Xmx512m")
                .withStartTimeout(1, MINUTES)
                .withDownloadDirectory(new File("/tmp/embedded-elasticsearch-downloads"))
                .withCleanInstallationDirectoryOnStop(false)
                .build()
                .start();
        jestClient = new JestClientProvider(
                Collections.singletonList(URI.create("http://localhost:" + embeddedElastic.getHttpPort())),
                Duration.seconds(30),
                Duration.seconds(30),
                Duration.seconds(30),
                10,
                10,
                5,
                false,
                null,
                Duration.seconds(30),
                false,
                objectMapper
        ).get();

        this.indexSet = mock(IndexSet.class);
        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSet.getConfig()).thenReturn(indexSetConfig);
        when(indexSet.getWriteIndexAlias()).thenReturn(currentIndex);
        when(indexSet.getIndexWildcard()).thenReturn(indexName + "_*");
        when(indexSetConfig.indexAnalyzer()).thenReturn("standard");
        when(indexSetConfig.indexTemplateName()).thenReturn(indexName);
        when(indexSetConfig.indexWildcard()).thenReturn(indexName + "_*");
        when(indexSetConfig.replicas()).thenReturn(1);
        when(indexSetConfig.shards()).thenReturn(1);
    }

    private Messages messages;

    @Before
    public void setUpClasses() {
        final IndexMappingFactory indexMappingFactory = new IndexMappingFactory(new Node(jestClient));
        final NodeId nodeId = mock(NodeId.class);
        when(nodeId.toString()).thenReturn("deadbeef");
        final AuditEventSender auditEventSender = mock(AuditEventSender.class);
        indices = new Indices(jestClient, objectMapper, indexMappingFactory, null, nodeId, auditEventSender, null);

        final MetricRegistry metricRegistry = new MetricRegistry();
        messages = new Messages(metricRegistry, jestClient);
        indices.create(currentIndex, indexSet);
    }

    @Test
    public void verifyProperTypingForIndexedMessages() throws Exception {
        indexMessage(ImmutableMap.of(
                "gl2_boolean", true,
                "gl2_number", 42,
                "gl2_array", ImmutableList.of(23, 42)
                ));

        assertThat(searchFor("gl2_boolean:true").getTotal()).isEqualTo(1);
        assertThat(searchFor("gl2_number:[1 TO 23]").getTotal()).isEqualTo(0);
        assertThat(searchFor("gl2_number:[23 TO 42]").getTotal()).isEqualTo(1);
        assertThat(searchFor("gl2_array:23").getTotal()).isEqualTo(1);
        assertThat(searchFor("gl2_array:42").getTotal()).isEqualTo(1);
    }

    private void indexMessage(Map<String, Object> additionalFields) throws InterruptedException, IOException {
        final Message message = new Message("foo", "bar", DateTime.now());
        additionalFields.forEach(message::addField);
        final List<Map.Entry<IndexSet, Message>> messageList = Collections.singletonList(new AbstractMap.SimpleEntry<>(indexSet, message));
        final List<String> messageIds = messages.bulkIndex(messageList);

        embeddedElastic.refreshIndices();

        assertThat(messageIds).isEmpty();
    }

    private String queryFor(String query) {
        return new SearchSourceBuilder()
                .query(new QueryStringQueryBuilder(query))
                .toString();
    }

    private SearchResult searchFor(String query) throws IOException {
        final Search search = new Search.Builder(queryFor(query))
                .addIndex(currentIndex)
                .addType(IndexMapping.TYPE_MESSAGE)
                .build();
        return jestClient.execute(search);
    }
}
