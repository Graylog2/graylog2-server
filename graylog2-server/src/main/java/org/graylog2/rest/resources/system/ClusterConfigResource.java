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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.models.system.config.ClusterConfigList;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.hibernate.validator.constraints.NotBlank;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Api(value = "System/ClusterConfig", description = "Graylog Cluster Configuration")
@RequiresAuthentication
@Path("/system/cluster_config")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterConfigResource extends RestResource {
    private final ClusterConfigService clusterConfigService;
    private final ChainingClassLoader chainingClassLoader;

    @Inject
    public ClusterConfigResource(ClusterConfigService clusterConfigService, ChainingClassLoader chainingClassLoader) {
        this.clusterConfigService = requireNonNull(clusterConfigService);
        this.chainingClassLoader = chainingClassLoader;
    }

    @GET
    @ApiOperation(value = "List all configuration classes")
    @Timed
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_READ)
    public ClusterConfigList list() {
        final Set<Class<?>> classes = clusterConfigService.list();

        return ClusterConfigList.createFromClass(classes);
    }

    @GET
    @Path("{configClass}")
    @ApiOperation(value = "Get configuration settings from database")
    @Timed
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_READ)
    public Object read(@ApiParam(name = "configClass", value = "The name of the cluster configuration class", required = true)
                       @PathParam("configClass") @NotBlank String configClass) {
        final Class<?> cls = classFromName(configClass);
        if (cls == null) {
            throw new NotFoundException("Couldn't find configuration class \"" + configClass + "\"");
        }

        return clusterConfigService.get(cls);
    }

    @PUT
    @Timed
    @Path("{configClass}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update configuration in database")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    public Response update(@ApiParam(name = "configClass", value = "The name of the cluster configuration class", required = true)
                           @PathParam("configClass") @NotBlank String configClass,
                           @ApiParam(name = "body", value = "The payload of the cluster configuration", required = true)
                           @NotNull InputStream body) throws IOException {
        final Class<?> cls = classFromName(configClass);
        if (cls == null) {
            throw new NotFoundException("Couldn't find configuration class \"" + configClass + "\"");
        }

        final Object o = objectMapper.readValue(body, cls);
        clusterConfigService.write(o);

        return Response.accepted(o).build();
    }

    @DELETE
    @Path("{configClass}")
    @ApiOperation(value = "Delete configuration settings from database")
    @Timed
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_DELETE)
    public void delete(@ApiParam(name = "configClass", value = "The name of the cluster configuration class", required = true)
                       @PathParam("configClass") @NotBlank String configClass) {
        final Class<?> cls = classFromName(configClass);
        if (cls == null) {
            throw new NotFoundException("Couldn't find configuration class \"" + configClass + "\"");
        }

        clusterConfigService.remove(cls);
    }

    @GET
    @Path("{configClass}")
    @Produces(MoreMediaTypes.APPLICATION_SCHEMA_JSON)
    @ApiOperation(value = "Get JSON schema of configuration class")
    @Timed
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_READ)
    public JsonSchema schema(@ApiParam(name = "configClass", value = "The name of the cluster configuration class", required = true)
                             @PathParam("configClass") @NotBlank String configClass) {
        final Class<?> cls = classFromName(configClass);
        if (cls == null) {
            throw new NotFoundException("Couldn't find configuration class \"" + configClass + "\"");
        }

        final SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
        try {
            objectMapper.acceptJsonFormatVisitor(objectMapper.constructType(cls), visitor);
        } catch (JsonMappingException e) {
            throw new InternalServerErrorException("Couldn't generate JSON schema for configuration class " + configClass, e);
        }

        return visitor.finalSchema();
    }

    @Nullable
    private Class<?> classFromName(String className) {
        try {
            return chainingClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
