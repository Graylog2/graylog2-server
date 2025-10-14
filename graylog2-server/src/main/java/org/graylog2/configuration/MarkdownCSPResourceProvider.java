package org.graylog2.configuration;

import jakarta.inject.Inject;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.resources.csp.CSPResources;

import java.util.Optional;
import java.util.Set;

public class MarkdownCSPResourceProvider implements CSPResources.ResourceProvider {
    private final ClusterConfigService configService;


    @Inject
    public MarkdownCSPResourceProvider(ClusterConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String type() {
        return "img-src";
    }

    @Override
    public Set<String> resources() {
        return Optional.ofNullable(configService.get(MarkdownConfiguration.class))
                .map(c -> c.allowAllImageSources() ? Set.of("*") : Set.of(c.allowedImageSources().split(",")))
                .orElse(Set.of());
    }
}
