/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.streams.alerts;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.streams.Stream;
import com.wordnik.swagger.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@RequiresAuthentication
@Api(value = "AlertReceivers", description = "Manage stream alert receivers")
@Path("/streams/{streamId}/alerts")
public class StreamAlertReceiverResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(StreamAlertReceiverResource.class);

    private final StreamService streamService;
    private final AlarmCallbackConfigurationService alarmCallbackConfigurationService;
    private final EmailAlarmCallback emailAlarmCallback;
    private final AlarmCallbackFactory alarmCallbackFactory;

    @Inject
    public StreamAlertReceiverResource(StreamService streamService,
                                       AlarmCallbackConfigurationService alarmCallbackConfigurationService,
                                       EmailAlarmCallback emailAlarmCallback,
                                       AlarmCallbackFactory alarmCallbackFactory) {
        this.streamService = streamService;
        this.alarmCallbackConfigurationService = alarmCallbackConfigurationService;
        this.emailAlarmCallback = emailAlarmCallback;
        this.alarmCallbackFactory = alarmCallbackFactory;
    }

    @POST
    @Timed
    @Path("receivers")
    @ApiOperation(value = "Add an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response addReceiver(
            @ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
            @ApiParam(name = "entity", value = "Name/ID of user or email address to add as alert receiver.", required = true) @QueryParam("entity") String entity,
            @ApiParam(name = "type", value = "Type: users or emails", required = true) @QueryParam("type") String type
    ) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        if(type == null || (!type.equals("users") && !type.equals("emails"))) {
            LOG.warn("No such type: [{}]", type);
            throw new WebApplicationException(400);
        }

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        // Maybe the list already contains this receiver?
        if (stream.getAlertReceivers().containsKey(type) || stream.getAlertReceivers().get(type) != null) {
            if (stream.getAlertReceivers().get(type).contains(entity)) {
                return Response.status(Response.Status.CREATED).build();
            }
        }

        streamService.addAlertReceiver(stream, type, entity);

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE @Timed
    @Path("receivers")
    @ApiOperation(value = "Remove an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response removeReceiver(
            @ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
            @ApiParam(name = "entity", value = "Name/ID of user or email address to remove from alert receivers.", required = true) @QueryParam("entity") String entity,
            @ApiParam(name = "type", value = "Type: users or emails", required = true) @QueryParam("type") String type) {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        if(!type.equals("users") && !type.equals("emails")) {
            LOG.warn("No such type: [{}]", type);
            throw new WebApplicationException(400);
        }

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        streamService.removeAlertReceiver(stream, type, entity);

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET @Timed
    @Path("sendDummyAlert")
    @ApiOperation(value = "Send a test mail for a given stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response sendDummyAlert(@ApiParam(name = "streamId",
            value = "The stream id this new alert condition belongs to.",
            required = true) @PathParam("streamId") String streamid)
            throws TransportConfigurationException, EmailException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        Stream stream;
        try {
            stream = streamService.load(streamid);
        } catch (org.graylog2.database.NotFoundException e) {
            throw new WebApplicationException(404);
        }

        Map<String, Object> parameters = Maps.newHashMap();
        DummyAlertCondition dummyAlertCondition = new DummyAlertCondition(stream, null, Tools.iso8601(), getSubject().getPrincipal().toString(), parameters);

        try {
            AbstractAlertCondition.CheckResult checkResult = dummyAlertCondition.runCheck();
            List<AlarmCallbackConfiguration> callConfigurations = alarmCallbackConfigurationService.getForStream(stream);
            if (callConfigurations.size() > 0)
                for (AlarmCallbackConfiguration configuration : callConfigurations) {
                    AlarmCallback alarmCallback = alarmCallbackFactory.create(configuration);
                    alarmCallback.call(stream, checkResult);
                }
            else
                emailAlarmCallback.call(stream, checkResult);

        } catch (AlarmCallbackException | ClassNotFoundException | AlarmCallbackConfigurationException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
