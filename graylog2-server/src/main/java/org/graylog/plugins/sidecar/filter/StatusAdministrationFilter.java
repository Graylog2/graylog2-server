package org.graylog.plugins.sidecar.filter;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.sidecar.rest.models.CollectorStatusList;
import org.graylog.plugins.sidecar.rest.models.Sidecar;

import javax.inject.Inject;

public class StatusAdministrationFilter implements AdministrationFilter {
    private final int status;

    @Inject
    public StatusAdministrationFilter(@Assisted int status) {
        this.status = status;
    }

    @Override
    public boolean test(Sidecar sidecar) {
        final CollectorStatusList collectorStatusList = sidecar.nodeDetails().statusList();
        if (collectorStatusList == null) {
            return false;
        }
        return collectorStatusList.collectors().entrySet().stream().anyMatch(entry -> entry.getValue().status() == status);
    }
}
