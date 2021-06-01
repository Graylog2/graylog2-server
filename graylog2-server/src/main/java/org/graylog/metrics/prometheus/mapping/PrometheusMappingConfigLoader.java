package org.graylog.metrics.prometheus.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.prometheus.client.dropwizard.samplebuilder.MapperConfig;
import org.graylog2.plugin.system.NodeId;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PrometheusMappingConfigLoader {
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final NodeId nodeId;

    @Inject
    public PrometheusMappingConfigLoader(NodeId nodeId) {
        this.nodeId = nodeId;
    }


    public Set<MapperConfig> load(byte[] bytes) throws IOException {
        final PrometheusMappingConfig config = YAML_MAPPER.readValue(new ByteArrayInputStream(bytes), PrometheusMappingConfig.class);

        return config.metricMappings().stream()
                .map(this::mapMetric)
                .collect(Collectors.toSet());
    }

    private MapperConfig mapMetric(PrometheusMappingConfig.MetricMapping mapping) {
        final Map<String, String> labels = new HashMap<>();

        // Add nodeId to every metric.
        // TODO: Can prometheus do this with some global label?
        labels.put("node", nodeId.toString());
        labels.putAll(mapping.additionalLabels());

        for (int i = 0; i < mapping.wildcardExtractLabels().size(); i++) {
            labels.put(mapping.wildcardExtractLabels().get(i), "${" + i + "}");
        }

        return new MapperConfig(mapping.matchPattern(), "gl_" + mapping.metricName(), labels);
    }
}
