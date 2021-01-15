/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.sidecar.filter;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.sidecar.rest.models.CollectorStatusList;
import org.graylog.plugins.sidecar.rest.models.Sidecar;

import javax.inject.Inject;

public class StatusAdministrationFilter implements AdministrationFilter {
    private final Sidecar.Status status;

    @Inject
    public StatusAdministrationFilter(@Assisted int status) {
        this.status = Sidecar.Status.fromStatusCode(status);
    }

    @Override
    public boolean test(Sidecar sidecar) {
        final CollectorStatusList collectorStatusList = sidecar.nodeDetails().statusList();
        if (collectorStatusList == null) {
            // Sidecars with not known status are in an UNKNOWN status
            return Sidecar.Status.UNKNOWN.equals(status);
        }
        return collectorStatusList.collectors().stream()
                .anyMatch(status -> Sidecar.Status.fromStatusCode(status.status()).equals(this.status));
    }
}
