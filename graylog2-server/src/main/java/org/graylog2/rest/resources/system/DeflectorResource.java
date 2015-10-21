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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.Deflector;
import org.graylog2.rest.models.system.responses.TimeBasedRotationStrategyResponse;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.security.RestPermissions;
import org.graylog2.indexer.rotation.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.TimeBasedRotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.rest.models.system.responses.DeflectorConfigResponse;
import org.graylog2.rest.models.system.responses.MessageCountRotationStrategyResponse;
import org.graylog2.rest.models.system.responses.SizeBasedRotationStrategyResponse;
import org.graylog2.security.RestrictToMaster;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/Deflector", description = "Index deflector management")
@Path("/system/deflector")
public class DeflectorResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeflectorResource.class);

    private final Deflector deflector;
    private final ActivityWriter activityWriter;
    private final Provider<RotationStrategy> rotationStrategyProvider;
    private final ElasticsearchConfiguration configuration;

    @Inject
    public DeflectorResource(Deflector deflector,
                             ActivityWriter activityWriter,
                             Provider<RotationStrategy> rotationStrategyProvider,
                             ElasticsearchConfiguration configuration) {
        this.deflector = deflector;
        this.activityWriter = activityWriter;
        this.rotationStrategyProvider = rotationStrategyProvider;
        this.configuration = configuration;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get current deflector status")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public DeflectorSummary deflector() {
        return DeflectorSummary.create(deflector.isUp(), deflector.getCurrentActualTargetIndex());
    }

    @GET
    @Timed
    @ApiOperation(value = "Get deflector configuration. Only available on master nodes.")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Path("/config")
    @Produces(MediaType.APPLICATION_JSON)
    @RestrictToMaster
    public DeflectorConfigResponse config() {
        final RotationStrategy strategy = rotationStrategyProvider.get();
        DeflectorConfigResponse response = null;

        if (strategy instanceof MessageCountRotationStrategy) {
            response = new MessageCountRotationStrategyResponse(configuration.getMaxDocsPerIndex());
        } else if (strategy instanceof SizeBasedRotationStrategy) {
            response = new SizeBasedRotationStrategyResponse(configuration.getMaxSizePerIndex());
        } else if (strategy instanceof TimeBasedRotationStrategy) {
            response = new TimeBasedRotationStrategyResponse(configuration.getMaxTimePerIndex());
        } else {
            throw new InternalServerErrorException("Unknown rotation strategy!");
        }

        response.maxNumberOfIndices = configuration.getMaxNumberOfIndices();

        return response;
    }

    @POST
    @Timed
    @ApiOperation(value = "Cycle deflector to new/next index")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/cycle")
    @RestrictToMaster
    public void cycle() {
        final String msg = "Cycling deflector. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DeflectorResource.class));

        deflector.cycle();
    }
}
