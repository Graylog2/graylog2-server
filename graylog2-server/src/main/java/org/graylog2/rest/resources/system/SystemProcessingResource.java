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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.ProcessingPauseLockedException;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@RequiresAuthentication
@Api(value = "System/Processing", description = "System processing status control.")
@Path("/system/processing")
public class SystemProcessingResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SystemProcessingResource.class);

    private final ServerStatus serverStatus;

    @Inject
    public SystemProcessingResource(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    // TODO Change to @POST
    @PUT
    @Timed
    @ApiOperation(value = "Pauses message processing",
            notes = "If the message journal is enabled, incoming messages will be spooled on disk, if it is disabled, " +
                    "you might lose messages from inputs which cannot buffer themselves, like AMQP or Kafka-based inputs.")
    @Path("pause")
    public void pauseProcessing() {
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, serverStatus.getNodeId().toString());
        serverStatus.pauseMessageProcessing(false);

        LOG.info("Paused message processing - triggered by REST call.");
    }

    @PUT
    @Timed
    @ApiOperation(value = "Resume message processing")
    @Path("resume")
    public void resumeProcessing() {
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, serverStatus.getNodeId().toString());

        try {
            serverStatus.resumeMessageProcessing();
        } catch (ProcessingPauseLockedException e) {
            LOG.error("Message processing pause is locked. Returning HTTP 403.");
            throw new ForbiddenException(e);
        }

        LOG.info("Resumed message processing - triggered by REST call.");
    }

    @PUT
    @Timed
    @Path("pause/unlock")
    public void unlockProcessingPause() {
        /*
         * This is meant to be only used in exceptional cases, when something that locked the processing pause
         * has crashed and never unlocked so we need to unlock manually. #donttellanybody
         */
        checkPermission(RestPermissions.PROCESSING_CHANGESTATE, serverStatus.getNodeId().toString());

        serverStatus.unlockProcessingPause();

        LOG.info("Manually unlocked message processing pause - triggered by REST call.");
    }
}
