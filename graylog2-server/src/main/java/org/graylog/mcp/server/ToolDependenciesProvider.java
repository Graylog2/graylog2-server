package org.graylog.mcp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import org.graylog.mcp.config.McpConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;

import static org.graylog.mcp.config.McpConfiguration.DEFAULT_VALUES;

public class ToolDependenciesProvider {

    private final ObjectMapper objectMapper;
    private final ClusterConfigService clusterConfigService;
    private final SchemaGeneratorProvider schemaGeneratorProvider;

    public ToolDependenciesProvider(ObjectMapper objectMapper, SchemaGeneratorProvider schemaGeneratorProvider) {
        this(objectMapper, schemaGeneratorProvider, null);
    }

    public ToolDependenciesProvider(ObjectMapper objectMapper,
                                    SchemaGeneratorProvider schemaGeneratorProvider,
                                    ClusterConfigService clusterConfigService) {
        this.objectMapper = objectMapper;
        this.schemaGeneratorProvider = schemaGeneratorProvider;
        this.clusterConfigService = clusterConfigService;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public SchemaGenerator getSchemaGenerator() {
        return schemaGeneratorProvider.get();
    }

    public McpConfiguration getMcpConfiguration() {
        if (clusterConfigService == null) {
            return McpConfiguration.create(DEFAULT_VALUES.enableRemoteAccess(), DEFAULT_VALUES.enableOutputSchema());
        }
        return clusterConfigService.getOrDefault(McpConfiguration.class, DEFAULT_VALUES);
    }

}
