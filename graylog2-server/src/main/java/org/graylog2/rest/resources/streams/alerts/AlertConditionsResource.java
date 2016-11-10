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
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.ConfigurableTypeInfo;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "AlertConditions", description = "Manage stream alert conditions")
@Path("/alerts/conditions")
@Produces(MediaType.APPLICATION_JSON)
public class AlertConditionsResource extends RestResource {
    private final Map<String, AlertCondition.Factory> alertConditionTypesMap;

    @Inject
    public AlertConditionsResource(Map<String, AlertCondition.Factory> alertConditionTypesMap) {
        this.alertConditionTypesMap = alertConditionTypesMap;
    }

    @GET
    @Path("/types")
    @Timed
    @ApiOperation(value = "Get a list of all alert condition types")
    public Map<String, ConfigurableTypeInfo> available() {
        return this.alertConditionTypesMap
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> ConfigurableTypeInfo.create(entry.getKey(), entry.getValue().descriptor(), entry.getValue().config().getRequestedConfiguration())
            ));
    }
}
