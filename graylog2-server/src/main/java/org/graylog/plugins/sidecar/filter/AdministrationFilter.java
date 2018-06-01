package org.graylog.plugins.sidecar.filter;

import com.google.inject.name.Named;
import org.graylog.plugins.sidecar.rest.models.Sidecar;

import java.util.function.Predicate;

public interface AdministrationFilter extends Predicate<Sidecar> {
    enum Type {
        COLLECTOR, CONFIGURATION, OS, STATUS
    }

    interface Factory {
        @Named("collector") AdministrationFilter createCollectorFilter(String collectorId);
        @Named("configuration") AdministrationFilter createConfigurationFilter(String configurationId);
        @Named("os") AdministrationFilter createOsFilter(String os);
        @Named("status") AdministrationFilter createStatusFilter(int status);
    }
}
