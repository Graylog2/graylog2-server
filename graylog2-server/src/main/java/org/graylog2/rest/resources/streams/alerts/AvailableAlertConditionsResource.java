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
@Api(value = "AlertConditions", description = "Available Alert Condition Types")
@Path("/alerts/conditions/types")
public class AvailableAlertConditionsResource extends RestResource {
    private final Map<String, AlertCondition.Factory> alertConditionTypesMap;

    @Inject
    public AvailableAlertConditionsResource(Map<String, AlertCondition.Factory> alertConditionTypesMap) {
        this.alertConditionTypesMap = alertConditionTypesMap;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alarm callback types")
    @Produces(MediaType.APPLICATION_JSON)
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
