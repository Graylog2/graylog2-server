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
package org.graylog2.rest.resources.count;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.counts.Counts;
import org.graylog2.rest.models.count.responses.MessageCountResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "Counts", description = "Message counts")
@Path("/count")
public class CountResource extends RestResource {
    private final Counts counts;
    private final IndexSetRegistry indexSetRegistry;

    @Inject
    public CountResource(Counts counts, IndexSetRegistry indexSetRegistry) {
        this.counts = counts;
        this.indexSetRegistry = indexSetRegistry;
    }

    @GET
    @Path("/total")
    @Timed
    @RequiresPermissions(RestPermissions.MESSAGECOUNT_READ)
    @ApiOperation(value = "Total number of messages in all your indices.")
    @Produces(MediaType.APPLICATION_JSON)
    public MessageCountResponse total() {
        return MessageCountResponse.create(counts.total());
    }

    @GET
    @Path("/{indexSetId}/total")
    @Timed
    @RequiresPermissions(RestPermissions.MESSAGECOUNT_READ)
    @ApiOperation(value = "Total number of messages in all your indices.")
    @Produces(MediaType.APPLICATION_JSON)
    public MessageCountResponse total(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) {
        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        return MessageCountResponse.create(counts.total(indexSet));
    }
}
