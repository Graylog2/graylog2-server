/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.system;

import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog.plugins.map.geoip.GeoIpResolver;
import org.graylog.plugins.map.geoip.GeoIpVendorResolverService;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.models.system.config.ClusterConfigList;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
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
import java.util.Locale;
import java.util.Set;

import static java.util.Objects.requireNonNull;

@Api(value = "System/ClusterConfig")
@RequiresAuthentication
@Path("/system/cluster_config")
@Produces(MediaType.APPLICATION_JSON)
public class ClusterConfigResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigResource.class);
    public static final String NO_CLASS_MSG = "Couldn't find configuration class  '%s'";

    private final ClusterConfigService clusterConfigService;
    private final ChainingClassLoader chainingClassLoader;
    private final ObjectMapper objectMapper;
    private final GeoIpVendorResolverService geoIpVendorResolverService;

    @Inject
    public ClusterConfigResource(ClusterConfigService clusterConfigService,
                                 ChainingClassLoader chainingClassLoader,
                                 ObjectMapper objectMapper,
                                 GeoIpVendorResolverService geoIpVendorResolverService) {
        this.clusterConfigService = requireNonNull(clusterConfigService);
        this.chainingClassLoader = chainingClassLoader;
        this.objectMapper = objectMapper;
        this.geoIpVendorResolverService = geoIpVendorResolverService;
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
            String error = createNoClassMsg(configClass);
            throw new NotFoundException(error);
        }

        return clusterConfigService.get(cls);
    }

    @PUT
    @Timed
    @Path("{configClass}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update configuration in database")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_UPDATE)
    public Response update(@ApiParam(name = "configClass", value = "The name of the cluster configuration class", required = true)
                           @PathParam("configClass") @NotBlank String configClass,
                           @ApiParam(name = "body", value = "The payload of the cluster configuration", required = true)
                           @NotNull InputStream body) throws IOException {
        final Class<?> cls = classFromName(configClass);
        if (cls == null) {
            throw new NotFoundException(createNoClassMsg(configClass));
        }

        final Object o;
        try {
            o = objectMapper.readValue(body, cls);
        } catch (Exception e) {
            final String msg = "Couldn't parse cluster configuration \"" + configClass + "\".";
            LOG.error(msg, e);
            throw new BadRequestException(msg);
        }

        validateIfGeoIpResolverConfig(o);

        try {
            clusterConfigService.write(o);
        } catch (Exception e) {
            final String msg = "Couldn't write cluster config \"" + configClass + "\".";
            LOG.error(msg, e);
            throw new InternalServerErrorException(msg, e);
        }

        return Response.accepted(o).build();
    }

    private void validateIfGeoIpResolverConfig(Object o) {
        if (o instanceof GeoIpResolverConfig) {
            final GeoIpResolverConfig config = (GeoIpResolverConfig) o;

            Timer timer = new Timer(new UniformReservoir());
            try {
                GeoIpResolver<?> cityResolver = geoIpVendorResolverService.createCityResolver(config, timer);
                if (config.enabled() && !cityResolver.isEnabled()) {
                    String msg = String.format(Locale.ENGLISH, "Invalid '%s' City Geo IP database file '%s'.  Make sure the file exists and is valid for '%1$s'", config.databaseVendorType(), config.cityDbPath());
                    throw new IllegalArgumentException(msg);
                }

                //ASN file is optional--do not validate if not provided.
                if (!(config.asnDbPath() == null || config.asnDbPath().trim().isEmpty())) {
                    GeoIpResolver<?> asnResolver = geoIpVendorResolverService.createAsnResolver(config, timer);
                    if (config.enabled() && !asnResolver.isEnabled()) {
                        String msg = String.format(Locale.ENGLISH, "Invalid '%s'  ASN database file '%s'.  Make sure the file exists and is valid for '%1$s'", config.databaseVendorType(), config.asnDbPath());
                        throw new IllegalArgumentException(msg);
                    }
                }
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage(), e);
                throw new BadRequestException(e.getMessage());
            }
        }
    }

    @DELETE
    @Path("{configClass}")
    @ApiOperation(value = "Delete configuration settings from database")
    @Timed
    @RequiresPermissions(RestPermissions.CLUSTER_CONFIG_ENTRY_DELETE)
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_DELETE)
    public void delete(@ApiParam(name = "configClass", value = "The name of the cluster configuration class", required = true)
                       @PathParam("configClass") @NotBlank String configClass) {
        final Class<?> cls = classFromName(configClass);
        if (cls == null) {
            throw new NotFoundException(createNoClassMsg(configClass));
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
            throw new NotFoundException(createNoClassMsg(configClass));
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

    private static String createNoClassMsg(String configClass) {
        return String.format(Locale.ENGLISH, NO_CLASS_MSG, configClass);
    }

}
