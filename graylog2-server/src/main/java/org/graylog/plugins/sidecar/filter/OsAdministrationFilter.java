package org.graylog.plugins.sidecar.filter;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.sidecar.rest.models.Sidecar;

import javax.inject.Inject;

public class OsAdministrationFilter implements AdministrationFilter {
    private final String os;

    @Inject
    public OsAdministrationFilter(@Assisted String os) {
        this.os = os;
    }

    @Override
    public boolean test(Sidecar sidecar) {
        return sidecar.nodeDetails().operatingSystem().equalsIgnoreCase(os);
    }
}
