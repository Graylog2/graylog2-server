package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import jakarta.json.Json;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettingsFactory;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.cluster.GetClusterSettingsResponse;

import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterAdapterOS2Test {
    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private static final Map<String, JsonData> ENABLED_RESPONSE = Map.of("cluster", m(
            Map.of("routing",
                    Map.of("allocation",
                            Map.of("disk",
                                    Map.of(
                                            "threshold_enabled", "true",
                                            "watermark", Map.of(
                                                    "flood_stage", "95%",
                                                    "high", "90%",
                                                    "low", "85%",
                                                    "enable_for_single_data_node", "false"
                                            )
                                    ))))

    ));

    private static final Map<String, JsonData> DISABLED_RESPONSE = Map.of("cluster", m(
            Map.of("routing",
                    Map.of("allocation",
                            Map.of("disk",
                                    Map.of(
                                            "threshold_enabled", "false"
                                    ))))

    ));
    public static final ClusterAllocationDiskSettings ENABLED_RESULT = ClusterAllocationDiskSettingsFactory.create(
            true,
            "85%",
            "90%",
            "95%"
    );
    public static final ClusterAllocationDiskSettings DISABLED_RESULT = ClusterAllocationDiskSettingsFactory.create(false, null, null, null);

    private org.opensearch.client.opensearch.OpenSearchClient opensearchClient;

    private ClusterAdapter clusterAdapter;

    @BeforeEach
    void setup() {
        this.opensearchClient = mock(org.opensearch.client.opensearch.OpenSearchClient.class, RETURNS_DEEP_STUBS);
        final var client = new OpenSearchClient(mock(RestHighLevelClient.class), opensearchClient, mock(RestClient.class), objectMapper);
        this.clusterAdapter = new ClusterAdapterOS2(client, Duration.seconds(30), new PlainJsonApi(objectMapper, client));
    }

    @Test
    void enabledWatermarkSettingsFromDefaults() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(Map.of())
                .transient_(Map.of())
                .defaults(ENABLED_RESPONSE)
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(ENABLED_RESULT);
    }

    @Test
    void enabledWatermarkSettingsFromPersisted() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(ENABLED_RESPONSE)
                .transient_(Map.of())
                .defaults(Map.of())
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(ENABLED_RESULT);
    }

    @Test
    void enabledWatermarkSettingsFromTransient() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(Map.of())
                .transient_(ENABLED_RESPONSE)
                .defaults(Map.of())
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(ENABLED_RESULT);
    }

    @Test
    void disabledWatermarkSettingsFromDefaults() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(Map.of())
                .transient_(Map.of())
                .defaults(DISABLED_RESPONSE)
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(DISABLED_RESULT);
    }

    @Test
    void disabledWatermarkSettingsFromPersisted() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(DISABLED_RESPONSE)
                .transient_(Map.of())
                .defaults(Map.of())
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(DISABLED_RESULT);
    }

    @Test
    void disabledWatermarkSettingsFromTransient() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(Map.of())
                .transient_(DISABLED_RESPONSE)
                .defaults(Map.of())
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(DISABLED_RESULT);
    }

    @Test
    void missingWatermarkSettings() throws IOException {
        final var response = new GetClusterSettingsResponse.Builder()
                .persistent(Map.of())
                .transient_(Map.of())
                .defaults(Map.of("cluster", JsonData.of(Json.createObjectBuilder(
                        Map.of("routing",
                                Map.of("allocation", Map.of()))).build()

                )))
                .build();
        whenClusterSettings().thenReturn(response);

        final var result = clusterAdapter.clusterAllocationDiskSettings();

        assertThat(result).isEqualTo(DISABLED_RESULT);
    }

    private static JsonData m(Map<String, ?> response) {
        return JsonData.of(Json.createObjectBuilder(response).build());
    }

    private OngoingStubbing<GetClusterSettingsResponse> whenClusterSettings() throws IOException {
        return when(opensearchClient.cluster().getSettings(any(Function.class)));
    }
}
