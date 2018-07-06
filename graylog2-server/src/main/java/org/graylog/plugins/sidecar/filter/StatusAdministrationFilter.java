/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.sidecar.filter;

import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.sidecar.mapper.SidecarStatusMapper.Status;
import org.graylog.plugins.sidecar.rest.models.CollectorStatusList;
import org.graylog.plugins.sidecar.rest.models.Sidecar;

import javax.inject.Inject;

public class StatusAdministrationFilter implements AdministrationFilter {
    private final Status status;

    @Inject
    public StatusAdministrationFilter(@Assisted int status) {
        this.status = Status.fromStatusCode(status);
    }

    @Override
    public boolean test(Sidecar sidecar) {
        final CollectorStatusList collectorStatusList = sidecar.nodeDetails().statusList();
        if (collectorStatusList == null) {
            // Sidecars with not known status are in an UNKNOWN status
            return Status.UNKNOWN.equals(status);
        }
        return collectorStatusList.collectors().stream()
                .anyMatch(status -> Status.fromStatusCode(status.status()).equals(this.status));
    }
}
