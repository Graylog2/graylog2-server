package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.NodesInfo;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettingsFactory;
import org.graylog2.indexer.cluster.health.NodeDiskUsageStats;
import org.graylog2.indexer.cluster.health.NodeFileDescriptorStats;
import org.graylog2.indexer.cluster.jest.GetAllocationDiskSettings;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.indexer.indices.HealthStatus;
import org.graylog2.rest.models.system.indexer.responses.ClusterHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class ClusterAdapterES6 implements ClusterAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterAdapterES6.class);
    private final JestClient jestClient;
    private final Duration requestTimeout;

    @Inject
    public ClusterAdapterES6(JestClient jestClient,
                             @Named("elasticsearch_request_timeout") Duration requestTimeout) {
        this.jestClient = jestClient;
        this.requestTimeout = requestTimeout;
    }

    @Override
    public Optional<HealthStatus> health(List<String> indices) {
        final Optional<JsonNode> result = clusterHealth(indices);
        return result.map(this::extractHealthStatus);
    }

    private HealthStatus extractHealthStatus(JsonNode node) {
        final String statusString = node.path("status").asText().toLowerCase(Locale.ENGLISH);
        switch (statusString) {
            case "red": return HealthStatus.Red;
            case "yellow": return HealthStatus.Yellow;
            case "green": return HealthStatus.Green;
            default: throw new IllegalStateException("Unable to parse health status (known: green/yellow/red): " + statusString);
        }
    }

    @Override
    public Optional<HealthStatus> deflectorHealth(List<String> indices) {
        final Optional<JsonNode> result = clusterHealth(indices);
        return result.map(this::extractHealthStatus);
    }

    @Override
    public Set<NodeFileDescriptorStats> fileDescriptorStats() {
        final JsonNode nodes = catNodes("name", "host", "ip", "fileDescriptorMax");
        final ImmutableSet.Builder<NodeFileDescriptorStats> setBuilder = ImmutableSet.builder();
        for (JsonNode jsonElement : nodes) {
            if (jsonElement.isObject()) {
                final String name = jsonElement.path("name").asText();
                final String host = jsonElement.path("host").asText(null);
                final String ip = jsonElement.path("ip").asText();
                final JsonNode fileDescriptorMax = jsonElement.path("fileDescriptorMax");
                final Long maxFileDescriptors = fileDescriptorMax.isLong() ? fileDescriptorMax.asLong() : null;
                setBuilder.add(NodeFileDescriptorStats.create(name, ip, host, maxFileDescriptors));
            }
        }

        return setBuilder.build();
    }

    @Override
    public Set<NodeDiskUsageStats> diskUsageStats() {
        final JsonNode nodes = catNodes("name", "host", "ip", "diskUsed", "diskTotal","diskUsedPercent");
        final ImmutableSet.Builder<NodeDiskUsageStats> setBuilder = ImmutableSet.builder();
        for (JsonNode jsonElement : nodes) {
            if (jsonElement.isObject()) {
                setBuilder.add(
                        NodeDiskUsageStats.create(
                                jsonElement.path("name").asText(),
                                jsonElement.path("ip").asText(),
                                jsonElement.path("host").asText(null),
                                jsonElement.path("diskUsed").asText(),
                                jsonElement.path("diskTotal").asText(),
                                jsonElement.path("diskUsedPercent").asDouble(NodeDiskUsageStats.DEFAULT_DISK_USED_PERCENT)
                        )
                );
            }
        }
        return setBuilder.build();
    }

    @Override
    public ClusterAllocationDiskSettings clusterAllocationDiskSettings() {
        final GetAllocationDiskSettings request = new GetAllocationDiskSettings.Builder().build();
        final JestResult response = JestUtils.execute(jestClient, request, () -> "Unable to read Elasticsearch cluster settings");
        final JsonNode json = response.getJsonObject();
        final JsonNode floodStageSetting = findEffectiveSettingInSettingsGroups(json, "flood_stage", true);
        return ClusterAllocationDiskSettingsFactory.create(
                findEffectiveSettingInSettingsGroups(json, "threshold_enabled", false).asBoolean(),
                findEffectiveSettingInSettingsGroups(json, "low", false).asText(),
                findEffectiveSettingInSettingsGroups(json, "high", false).asText(),
                floodStageSetting != null ? floodStageSetting.asText() : ""
        );
    }

    @Override
    public Optional<String> nodeIdToName(String nodeId) {
        return Optional.ofNullable(getNodeInfo(nodeId).path("name").asText(null));
    }

    @Override
    public Optional<String> nodeIdToHostName(String nodeId) {
        return Optional.ofNullable(getNodeInfo(nodeId).path("host").asText(null));
    }

    @Override
    public boolean isConnected() {
        final Health request = new Health.Builder()
                .local()
                .timeout(Ints.saturatedCast(requestTimeout.toSeconds()))
                .build();

        try {
            final JestResult result = JestUtils.execute(jestClient, request, () -> "Couldn't check connection status of Elasticsearch");
            final int numberOfDataNodes = result.getJsonObject().path("number_of_data_nodes").asInt();
            return numberOfDataNodes > 0;
        } catch (ElasticsearchException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            return false;
        }
    }

    @Override
    public Optional<String> clusterName(List<String> indices) {
        return clusterHealth(indices).map(health -> health.path("cluster_name").asText("<unknown>"));
    }

    @Override
    public Optional<ClusterHealth> clusterHealthStats(List<String> indices) {
        return clusterHealth(indices).map(health -> {
            final ClusterHealth.ShardStatus shards = ClusterHealth.ShardStatus.create(
                    health.path("active_shards").asInt(),
                    health.path("initializing_shards").asInt(),
                    health.path("relocating_shards").asInt(),
                    health.path("unassigned_shards").asInt());

            return ClusterHealth.create(health.path("status").asText().toLowerCase(Locale.ENGLISH), shards);
        });
    }

    private JsonNode getNodeInfo(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return MissingNode.getInstance();
        }

        final NodesInfo request = new NodesInfo.Builder().addNode(nodeId).build();
        final JestResult result = JestUtils.execute(jestClient, request, () -> "Couldn't read information of Elasticsearch node " + nodeId);
        return result.getJsonObject().path("nodes").path(nodeId);
    }

    private JsonNode findEffectiveSettingInSettingsGroups(JsonNode jsonNode, String setting, boolean optional) {
        List<String> settingsGroup = Arrays.asList("transient", "persistent", "defaults");
        for(String group: settingsGroup) {
            JsonNode foundGroup = jsonNode.findPath(group);
            if (!(foundGroup instanceof MissingNode)) {
                JsonNode foundSetting = foundGroup.findPath(setting);
                if (!(foundSetting instanceof MissingNode)) {
                    return foundSetting;
                }
            }
        }
        if (optional) {
            return null;
        }
        throw new IllegalStateException(String.format(Locale.ENGLISH, "Could not find setting %s in Elasticsearch response", setting));
    }

    /**
     * Retrieve the response for the <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes</a> request from Elasticsearch.
     *
     * @param fields The fields to show, see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html">cat nodes API</a>.
     * @return A {@link JsonNode} with the result of the cat nodes request.
     */
    private JsonNode catNodes(String... fields) {
        final String fieldNames = String.join(",", fields);
        final Cat request = new Cat.NodesBuilder()
                .setParameter("h", fieldNames)
                .setParameter("full_id", true)
                .setParameter("format", "json")
                .build();
        final CatResult response = JestUtils.execute(jestClient, request, () -> "Unable to read Elasticsearch node information");
        return response.getJsonObject().path("result");
    }

    private Optional<JsonNode> clusterHealth(Collection<? extends String> indices) {
        final Health request = new Health.Builder()
                .addIndex(indices)
                .timeout(Ints.saturatedCast(requestTimeout.toSeconds()))
                .build();
        try {
            final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster health for indices " + indices);
            return Optional.of(jestResult.getJsonObject());
        } catch(ElasticsearchException e) {
            if (LOG.isDebugEnabled()) {
                LOG.error("{} ({})", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("n/a"), e);
            } else {
                LOG.error("{} ({})", e.getMessage(), Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("n/a"));
            }
            return Optional.empty();
        }
    }
}
