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
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.mail.EmailException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.common.Strings;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.AlarmCallbackFactory;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.alerts.types.DummyAlertCondition;
import org.graylog2.auditlog.jersey.AuditLog;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

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
    @AuditLog(object = "alert receiver")
    public Response addReceiver(
            @ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true)
            @PathParam("streamId") String streamid,
            @ApiParam(name = "entity", value = "Name/ID of user or email address to add as alert receiver.", required = true)
            @QueryParam("entity") String entity,
            @ApiParam(name = "type", value = "Type: users or emails", required = true)
            @QueryParam("type") String type
    ) throws org.graylog2.database.NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);
        checkArgument(!Strings.isNullOrEmpty(entity));

        if (type == null || (!type.equals("users") && !type.equals("emails"))) {
            final String msg = "No such type: [" + type + "]";
            LOG.warn(msg);
            throw new BadRequestException(msg);
        }

        final Stream stream = streamService.load(streamid);

        // TODO What's the actual URI of the created resource?
        final URI streamAlertUri = getUriBuilderToSelf().path(StreamAlertResource.class).build(streamid);

        // Maybe the list already contains this receiver?
        if (stream.getAlertReceivers().containsKey(type) || stream.getAlertReceivers().get(type) != null) {
            if (stream.getAlertReceivers().get(type).contains(entity)) {
                return Response.created(streamAlertUri).build();
            }
        }

        streamService.addAlertReceiver(stream, type, entity);

        return Response.created(streamAlertUri).build();
    }

    @DELETE
    @Timed
    @Path("receivers")
    @ApiOperation(value = "Remove an alert receiver")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    @AuditLog(object = "alert receiver")
    public void removeReceiver(
            @ApiParam(name = "streamId", value = "The stream id this new alert condition belongs to.", required = true) @PathParam("streamId") String streamid,
            @ApiParam(name = "entity", value = "Name/ID of user or email address to remove from alert receivers.", required = true) @QueryParam("entity") String entity,
            @ApiParam(name = "type", value = "Type: users or emails", required = true) @QueryParam("type") String type) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        if (!"users".equals(type) && !"emails".equals(type)) {
            final String msg = "No such type: [" + type + "]";
            LOG.warn(msg);
            throw new BadRequestException(msg);
        }

        final Stream stream = streamService.load(streamid);
        streamService.removeAlertReceiver(stream, type, entity);
    }

    @POST
    @Timed
    @Path("sendDummyAlert")
    @ApiOperation(value = "Send a test mail for a given stream")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Stream not found."),
            @ApiResponse(code = 400, message = "Invalid ObjectId.")
    })
    public Response sendDummyAlert(@ApiParam(name = "streamId", value = "The stream id the dummy alert should be sent for.", required = true)
                               @PathParam("streamId") String streamid)
            throws TransportConfigurationException, EmailException, NotFoundException {
        checkPermission(RestPermissions.STREAMS_EDIT, streamid);

        final Stream stream = streamService.load(streamid);

        final DummyAlertCondition dummyAlertCondition = new DummyAlertCondition(stream, null, Tools.nowUTC(), getSubject().getPrincipal().toString(), Collections.<String, Object>emptyMap(), "Dummy Alert");
        try {
            AbstractAlertCondition.CheckResult checkResult = dummyAlertCondition.runCheck();
            List<AlarmCallbackConfiguration> callConfigurations = alarmCallbackConfigurationService.getForStream(stream);
            if (callConfigurations.size() > 0)
                for (AlarmCallbackConfiguration configuration : callConfigurations) {
                    AlarmCallback alarmCallback = alarmCallbackFactory.create(configuration);
                    alarmCallback.call(stream, checkResult);
                }
            else {
                emailAlarmCallback.call(stream, checkResult);
            }
        } catch (AlarmCallbackException | ClassNotFoundException | AlarmCallbackConfigurationException e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        return Response.noContent().build();
    }
}
