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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.contentpacks.catalogs.CatalogIndex;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.rest.models.system.contenpacks.responses.CatalogIndexResponse;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@RequiresAuthentication
@Api(value = "System/Catalog", description = "Entity Catalog")
@Path("/system/catalog")
@Produces(MediaType.APPLICATION_JSON)
public class CatalogResource {
    private final CatalogIndex catalogIndex;

    @Inject
    public CatalogResource(CatalogIndex catalogIndex) {
        this.catalogIndex = catalogIndex;
    }

    @GET
    @Timed
    @ApiOperation(value = "List available entities in this Graylog cluster")
    @RequiresPermissions(RestPermissions.CATALOG_LIST)
    public CatalogIndexResponse showEntityIndex() {
        final Set<EntityExcerpt> entities = catalogIndex.entityIndex();
        return CatalogIndexResponse.create(entities);
    }
}
