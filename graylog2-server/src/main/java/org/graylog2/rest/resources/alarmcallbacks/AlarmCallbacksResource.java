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
package org.graylog2.rest.resources.alarmcallbacks;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackListSummary;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSummary;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbackSummaryResponse;
import org.graylog2.rest.models.alarmcallbacks.responses.AvailableAlarmCallbacksResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamService;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.STREAMS_READ;
import static org.graylog2.shared.security.RestPermissions.USERS_LIST;

@RequiresAuthentication
@Api(value = "AlarmCallbacks", description = "Manage alarm callbacks (aka alert notifications)")
@Path("/alerts/callbacks")
@Produces(MediaType.APPLICATION_JSON)
public class AlarmCallbacksResource extends RestResource {
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final StreamService streamService;
    private final Set<AlarmCallback> availableAlarmCallbacks;

    @Inject
    public AlarmCallbacksResource(AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                  StreamService streamService,
                                  Set<AlarmCallback> availableAlarmCallbacks) {
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.streamService = streamService;
        this.availableAlarmCallbacks = availableAlarmCallbacks;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks")
    public AlarmCallbackListSummary all() throws NotFoundException {
        final List<AlarmCallbackSummary> alarmCallbacks = streamService.loadAll().stream()
                .filter(stream -> isPermitted(STREAMS_READ, stream.getId()))
                .flatMap(stream -> alarmCallbackConfigurationService.getForStream(stream).stream()
                        .map(callback -> AlarmCallbackSummary.create(
                                callback.getId(),
                                callback.getStreamId(),
                                callback.getType(),
                                callback.getConfiguration(),
                                callback.getCreatedAt(),
                                callback.getCreatorUserId()
                        )))
                .collect(Collectors.toList());

        return AlarmCallbackListSummary.create(alarmCallbacks);
    }

    @GET
    @Path("/types")
    @Timed
    @ApiOperation(value = "Get a list of all alarm callbacks types")
    public AvailableAlarmCallbacksResponse available() {
        final Map<String, AvailableAlarmCallbackSummaryResponse> types = Maps.newHashMapWithExpectedSize(availableAlarmCallbacks.size());
        for (AlarmCallback availableAlarmCallback : availableAlarmCallbacks) {
            final AvailableAlarmCallbackSummaryResponse type = new AvailableAlarmCallbackSummaryResponse();
            type.name = availableAlarmCallback.getName();
            type.requested_configuration = getConfigurationRequest(availableAlarmCallback).asList();
            types.put(availableAlarmCallback.getClass().getCanonicalName(), type);
        }

        final AvailableAlarmCallbacksResponse response = new AvailableAlarmCallbacksResponse();
        response.types = types;

        return response;
    }

    /* This is used to add user auto-completion to EmailAlarmCallback when the current user has permissions to list users */
    private ConfigurationRequest getConfigurationRequest(AlarmCallback callback) {
        if (callback instanceof EmailAlarmCallback && isPermitted(USERS_LIST)) {
            return ((EmailAlarmCallback) callback).getEnrichedRequestedConfiguration();
        }

        return callback.getRequestedConfiguration();
    }
}
