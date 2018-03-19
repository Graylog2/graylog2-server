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
package org.graylog2.rest.resources.system.contentpacks;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.bundles.ContentPackService;
import org.graylog2.content_packs.ContentPack;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RequiresAuthentication
@Api(value = "System/ContentPacks", description = "Content Packs")
@Path("/system/content_packs")
@Produces(MediaType.APPLICATION_JSON)
public class ContentPackResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ContentPackResource.class);

    private final ContentPackService contentPackService;

    @Inject
    public ContentPackResource(final ContentPackService contentPackService) {
        this.contentPackService = contentPackService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available content packs")
    @ApiResponses(value = {
            @ApiResponse(code = 500, message = "Error loading content packs")
    })
    public Set<ContentPack> listBundles() {
        //TODO: [konrad] checkpermission!
        LOG.info("List content packs");
        Set<ContentPack> contentPacks = contentPackService.loadAll();

        return contentPacks;
    }
}
