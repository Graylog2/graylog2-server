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
