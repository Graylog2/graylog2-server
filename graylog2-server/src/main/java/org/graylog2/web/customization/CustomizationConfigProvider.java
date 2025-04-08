package org.graylog2.web.customization;

import com.google.common.base.Suppliers;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.function.Supplier;

@Singleton
public class CustomizationConfigProvider implements Provider<Config> {
    private final ClusterConfigService clusterConfigService;
    private final Supplier<Config> configSupplier;

    @Inject
    public CustomizationConfigProvider(ClusterConfigService clusterConfigService, @Named("isDevelopmentServer") boolean isDevelopment) {
        this.clusterConfigService = clusterConfigService;
        this.configSupplier = isDevelopment ? this::retrieve : Suppliers.memoize(this::retrieve);
    }

    private Config retrieve() {
        return clusterConfigService.get(Config.class);
    }

    @Override
    public Config get() {
        return configSupplier.get();
    }
}
