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
package org.graylog2.auditlog.jersey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.graylog2.auditlog.Actions;
import org.graylog2.auditlog.AuditLogger;
import org.graylog2.rest.RestTools;
import org.jboss.netty.handler.ipfilter.IpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@Provider
@Priority(Priorities.USER)
public class AuditLogFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AuditLogFilter.class);

    private final ResourceInfo resourceInfo;
    private final ExtendedUriInfo extendedUriInfo;
    private final Response response;
    private final AuditLogger auditLogger;
    private final Set<IpSubnet> trustedProxies;
    private final ObjectMapper objectMapper;

    private byte[] bufferedRequestEntitiy = null;

    @Inject
    public AuditLogFilter(@Context ResourceInfo resourceInfo,
                          @Context ExtendedUriInfo extendedUriInfo,
                          @Context Response response,
                          AuditLogger auditLogger,
                          @Named("trusted_proxies") Set<IpSubnet> trustedProxies,
                          ObjectMapper objectMapper) {
        this.resourceInfo = requireNonNull(resourceInfo);
        this.extendedUriInfo = requireNonNull(extendedUriInfo);
        this.response = requireNonNull(response);
        this.auditLogger = requireNonNull(auditLogger);
        this.trustedProxies = requireNonNull(trustedProxies);
        this.objectMapper = requireNonNull(objectMapper);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional.ofNullable(resourceInfo.getResourceMethod())
            .filter(method -> MediaType.APPLICATION_JSON_TYPE.equals(requestContext.getMediaType()))
            .map(m -> m.getAnnotation(AuditLog.class))
            .ifPresent(auditLog -> {
                if (requestContext.hasEntity() && auditLog.captureRequestEntity()) {
                    bufferedRequestEntitiy = bufferRequestEntity(requestContext);
                }
            });
    }

    @Nullable
    private byte[] bufferRequestEntity(ContainerRequestContext requestContext) {
        final InputStream entityStream = requestContext.getEntityStream();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = null;
        try {
            ByteStreams.copy(entityStream, baos);
            bytes = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            requestContext.setEntityStream(bais);
        } catch (IOException e) {
            LOG.debug("Error while buffering request entity", e);
        }

        return bytes;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Optional.ofNullable(resourceInfo.getResourceMethod())
            .map(m -> m.getAnnotation(AuditLog.class))
            .ifPresent(auditLog -> {
                final String originalSubject = auditLog.subject();
                final String subject = isNullOrEmpty(originalSubject) ? RestTools.getUserNameFromRequest(requestContext) : originalSubject;
                final String originalAction = auditLog.action();
                final String action = isNullOrEmpty(originalAction) ? getActionFromRequestMethod(requestContext.getMethod()) : originalAction;
                final String object = auditLog.object();

                final String remoteAddress = RestTools.getRemoteAddrFromRequest(response.getRequest(), trustedProxies);

                final Map<String, Object> context = new HashMap<>();
                context.put("remote_address", remoteAddress);

                if (auditLog.captureRequestContext()) {
                    final MultivaluedMap<String, String> pathParameters = extendedUriInfo.getPathParameters();
                    if (!pathParameters.isEmpty()) {
                        context.put("path_params", pathParameters);
                    }
                    final MultivaluedMap<String, String> queryParameters = extendedUriInfo.getQueryParameters();
                    if (!queryParameters.isEmpty()) {
                        context.put("query_params", queryParameters);
                    }
                }

                if (bufferedRequestEntitiy != null && auditLog.captureRequestEntity()) {
                    final Map<String, Object> requestEntity = readRequestEntity(bufferedRequestEntitiy);
                    if (requestEntity != null) {
                        context.put("request_entity", requestEntity);
                    }
                }

                if (responseContext.hasEntity() && auditLog.captureResponseEntity()) {
                    final Map<String, Object> responseEntity = readResponseEntity(responseContext);
                    if (responseEntity != null) {
                        context.put("response_entity", responseEntity);
                    }
                }

                switch (responseContext.getStatusInfo().getFamily()) {
                    case CLIENT_ERROR:
                    case SERVER_ERROR:
                        auditLogger.failure(subject, action, object, context);
                        break;
                    default:
                        auditLogger.success(subject, action, object, context);
                }
            });
    }

    @Nullable
    private Map<String, Object> readRequestEntity(byte[] requestEntity) {
        final TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        try {
            return objectMapper.readValue(requestEntity, typeRef);
        } catch (IOException e) {
            LOG.debug("Couldn't capture request entity", e);
        }
        return null;
    }

    @Nullable
    private Map<String, Object> readResponseEntity(ContainerResponseContext responseContext) {
        final Class<?> entityClass = responseContext.getEntityClass();
        final boolean isJson = MediaType.APPLICATION_JSON_TYPE.equals(responseContext.getMediaType());
        if (!entityClass.equals(Void.class) && !entityClass.equals(void.class) && isJson) {
            final Object entity = responseContext.getEntity();
            final TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };
            return objectMapper.convertValue(entity, typeRef);
        }

        return null;
    }

    private String getActionFromRequestMethod(String method) {
        switch (method) {
            case HttpMethod.GET:
                return Actions.READ;
            case HttpMethod.POST:
                return Actions.CREATE;
            case HttpMethod.PUT:
                return Actions.UPDATE;
            case HttpMethod.DELETE:
                return Actions.DELETE;
            default:
                return "unknown";
        }
    }
}
