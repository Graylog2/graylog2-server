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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Api(value = "Field Types")
@Path("/views/fields")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class FieldTypesResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(FieldTypesResource.class);
    private final MappedFieldTypesService mappedFieldTypesService;
    private final PermittedStreams permittedStreams;

    @Inject
    public FieldTypesResource(MappedFieldTypesService mappedFieldTypesService,
                              PermittedStreams permittedStreams) {
        this.mappedFieldTypesService = mappedFieldTypesService;
        this.permittedStreams = permittedStreams;
    }

    @GET
    @ApiOperation(value = "Retrieve the list of all fields present in the system")
    public Set<MappedFieldTypeDTO> allFieldTypes() {
        return mappedFieldTypesService.fieldTypesByStreamIds(permittedStreams.load(this::allowedToReadStream));
    }

    private boolean allowedToReadStream(String streamId) {
        return isPermitted(RestPermissions.STREAMS_READ, streamId);
    }

    @POST
    @ApiOperation(value = "Retrieve the field list of a given set of streams")
    @NoAuditEvent("This is not changing any data")
    public Set<MappedFieldTypeDTO> byStreams(FieldTypesForStreamsRequest request) {
        request.streams().forEach(this::checkStreamPermission);

        return mappedFieldTypesService.fieldTypesByStreamIds(request.streams());
    }

    private void checkStreamPermission(String instanceId) {
        if (!isPermitted(RestPermissions.STREAMS_READ, instanceId)) {
            LOG.info("Not authorized to access resource id <{}>. User <{}> is missing permission <{}:{}>",
                    instanceId, getSubject().getPrincipal(), RestPermissions.STREAMS_READ, instanceId);
            throw new MissingStreamPermissionException("Not authorized to access stream id <" + instanceId + ">",
                    ImmutableSet.of(instanceId));
        }
    }
}
