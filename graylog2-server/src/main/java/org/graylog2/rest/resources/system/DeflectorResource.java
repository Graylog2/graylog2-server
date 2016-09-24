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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.RestrictToMaster;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Deflector", description = "Index deflector management")
@Path("/system/deflector")
public class DeflectorResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeflectorResource.class);

    private final IndexSetRegistry indexSetRegistry;
    private final ActivityWriter activityWriter;
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration configuration;

    @Inject
    public DeflectorResource(IndexSetRegistry indexSetRegistry,
                             ActivityWriter activityWriter,
                             Map<String, Provider<RotationStrategy>> rotationStrategies,
                             ClusterConfigService clusterConfigService,
                             ElasticsearchConfiguration configuration) {
        this.indexSetRegistry = indexSetRegistry;
        this.activityWriter = activityWriter;
        this.rotationStrategies = rotationStrategies;
        this.clusterConfigService = clusterConfigService;
        this.configuration = configuration;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get current deflector status")
    @RequiresPermissions(RestPermissions.DEFLECTOR_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public DeflectorSummary deflector() throws TooManyAliasesException {
        // TODO 2.2: Resource needs to be adjusted to support multiple write targets
        final IndexSet indexSet = indexSetRegistry.getAllIndexSets().get(0);
        return DeflectorSummary.create(indexSet.isUp(), indexSet.getCurrentActualTargetIndex());
    }

    @POST
    @Timed
    @ApiOperation(value = "Cycle deflector to new/next index")
    @RequiresPermissions(RestPermissions.DEFLECTOR_CYCLE)
    @Path("/cycle")
    @RestrictToMaster
    @AuditEvent(type = AuditEventTypes.ES_WRITE_INDEX_UPDATE_JOB_START)
    public void cycle() {
        final String msg = "Cycling deflector. Reason: REST request.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, DeflectorResource.class));

        // TODO 2.2: Resource needs to be adjusted to support multiple write targets
        indexSetRegistry.getAllIndexSets().get(0).cycle();
    }
}
