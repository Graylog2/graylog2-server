package org.graylog.plugins.sidecar.filter;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;

import javax.inject.Inject;
import java.util.List;

public class ConfigurationAdministrationFilter implements AdministrationFilter {
    private final String configurationId;

    @Inject
    public ConfigurationAdministrationFilter(@Assisted String configurationId) {
        this.configurationId = configurationId;
    }

    @Override
    public boolean test(Sidecar sidecar) {
        final List<ConfigurationAssignment> assignments = sidecar.assignments();
        if (assignments == null) {
            return false;
        }
        return assignments.stream().anyMatch(assignment -> assignment.configurationId().equals(configurationId));
    }
}
