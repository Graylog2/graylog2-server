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

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.graylog2.auditlog.AuditLogger;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.ShiroPrincipal;
import org.graylog2.shared.security.ShiroSecurityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMapOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditLogFilterTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
    @Mock
    private ResourceInfo resourceInfo;
    @Mock
    private ExtendedUriInfo extendedUriInfo;
    @Mock
    private Response response;
    @Mock
    private Request request;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private ContainerResponseContext responseContext;

    private AuditLogFilter filter;

    @Before
    public void setUp() throws Exception {
        filter = new AuditLogFilter(resourceInfo, extendedUriInfo, response, auditLogger,
            Collections.emptySet(), objectMapperProvider.get());

        when(extendedUriInfo.getPathParameters()).thenReturn(new MultivaluedHashMap<>());
        when(extendedUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader(HttpHeaders.X_FORWARDED_FOR)).thenReturn("127.0.0.2");
        when(response.getRequest()).thenReturn(request);

        when(responseContext.getStatusInfo()).thenReturn(javax.ws.rs.core.Response.Status.OK);
    }

    @Test
    public void filterDoesNotLogIfResourceMethodIsNull() throws Exception {
        when(resourceInfo.getResourceMethod()).thenReturn(null);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, never()).success(anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
    }

    @Test
    public void filterDoesNotLogIfAuditLogAnnotationIsMissing() throws Exception {
        final Method resourceMethod = TestResource.class.getMethod("getWithoutAnnotation");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, never()).success(anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
    }

    @Test
    public void filterLogsFailureIfResponseCodeIs4xx() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("get", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(responseContext.getStatusInfo()).thenReturn(javax.ws.rs.core.Response.Status.BAD_REQUEST);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, never()).success(anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
        verify(auditLogger, only()).failure("subject", "read", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsFailureIfResponseCodeIs5xx() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("get", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        when(responseContext.getStatusInfo()).thenReturn(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, never()).success(anyString(), anyString(), anyString(), anyMapOf(String.class, Object.class));
        verify(auditLogger, only()).failure("subject", "read", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsActionDerivedFromGET() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("get", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "read", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsActionDerivedFromPOST() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.POST);
        final Method resourceMethod = TestResource.class.getMethod("post", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "created", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsActionDerivedFromPUT() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.PUT);
        final Method resourceMethod = TestResource.class.getMethod("put", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "updated", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsActionDerivedFromDELETE() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.DELETE);
        final Method resourceMethod = TestResource.class.getMethod("delete", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "deleted", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsActionDerivedFromOtherMethod() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.HEAD);
        final Method resourceMethod = TestResource.class.getMethod("head", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "unknown", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsActionGivenInAuditLogAnnotation() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("overrideAction");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "foobar", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsSubjectGivenInAuditLogAnnotation() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("overrideSubject");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "read", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsSubjectRetrievedFromRequest() throws Exception {
        final ShiroPrincipal shiroPrincipal = mock(ShiroPrincipal.class);
        when(shiroPrincipal.getName()).thenReturn("TEST");
        final ShiroSecurityContext shiroSecurityContext = mock(ShiroSecurityContext.class);
        when(shiroSecurityContext.getUserPrincipal()).thenReturn(shiroPrincipal);
        when(requestContext.getSecurityContext()).thenReturn(shiroSecurityContext);
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("detectSubject");
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("TEST", "action", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    public void filterLogsQueryParameters() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("get", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        final MultivaluedHashMap<String, String> queryParameters = new MultivaluedHashMap<>();
        queryParameters.putSingle("foo", "bar");
        when(extendedUriInfo.getQueryParameters()).thenReturn(queryParameters);

        filter.filter(requestContext, responseContext);

        final ImmutableMap<String, Object> expectedContext = ImmutableMap.of(
            "query_params", ImmutableMap.of("foo", Collections.singletonList("bar")),
            "remote_address", "127.0.0.1");
        verify(auditLogger, only()).success("subject", "read", "object", expectedContext);
    }

    @Test
    public void filterLogsPathParameters() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("get", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);
        final MultivaluedHashMap<String, String> pathParameters = new MultivaluedHashMap<>();
        pathParameters.putSingle("foo", "bar");
        when(extendedUriInfo.getPathParameters()).thenReturn(pathParameters);

        filter.filter(requestContext, responseContext);

        final ImmutableMap<String, Object> expectedContext = ImmutableMap.of(
            "path_params", ImmutableMap.of("foo", Collections.singletonList("bar")),
            "remote_address", "127.0.0.1");
        verify(auditLogger, only()).success("subject", "read", "object", expectedContext);
    }

    @Test
    public void filterLogsRemoteAddressIfContextCaptureIsDisabled() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        final Method resourceMethod = TestResource.class.getMethod("captureRequestContextDisabled", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        verify(auditLogger, only()).success("subject", "read", "object", Collections.singletonMap("remote_address", "127.0.0.1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterLogsRequestEntity() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.POST);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(requestContext.hasEntity()).thenReturn(true);
        // TODO: Write a real JerseyTest to avoid this "cheating"
        final InputStream entity = new ByteArrayInputStream("{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8));
        when(requestContext.getEntityStream()).thenReturn(entity);
        final Method resourceMethod = TestResource.class.getMethod("captureRequestEntity", Map.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        // Buffer request entity
        filter.filter(requestContext);

        // Create audit log entry
        filter.filter(requestContext, responseContext);

        final Map<String, Object> expectedContext = ImmutableMap.of(
            "request_entity", ImmutableMap.of("foo", "bar"),
            "remote_address", "127.0.0.1");
        verify(auditLogger, only()).success("subject", "created", "object", expectedContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterDoesNotLogRequestEntityWithWrongMediaType() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.POST);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        when(requestContext.hasEntity()).thenReturn(true);
        // TODO: Write a real JerseyTest to avoid this "cheating"
        final InputStream entity = new ByteArrayInputStream("{\"foo\":\"bar\"}".getBytes(StandardCharsets.UTF_8));
        when(requestContext.getEntityStream()).thenReturn(entity);
        final Method resourceMethod = TestResource.class.getMethod("captureRequestEntity", Map.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        // Buffer request entity
        filter.filter(requestContext);

        // Create audit log entry
        filter.filter(requestContext, responseContext);

        final Map<String, Object> expectedContext = ImmutableMap.of("remote_address", "127.0.0.1");
        verify(auditLogger, only()).success("subject", "created", "object", expectedContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterLogsResponseEntity() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(responseContext.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(responseContext.hasEntity()).thenReturn(true);
        // TODO: Write a real JerseyTest to avoid this "cheating"
        when(responseContext.getEntityClass()).thenReturn((Class) Map.class);
        when(responseContext.getEntity()).thenReturn(ImmutableMap.of("foo", "bar"));
        final Method resourceMethod = TestResource.class.getMethod("captureResponseEntity", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        final Map<String, Object> expectedContext = ImmutableMap.of(
            "response_entity", ImmutableMap.of("foo", "bar"),
            "remote_address", "127.0.0.1");
        verify(auditLogger, only()).success("subject", "read", "object", expectedContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void filterDoesNotLogResponseEntityWithWrongMediaType() throws Exception {
        when(requestContext.getMethod()).thenReturn(HttpMethod.GET);
        when(requestContext.getMediaType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        when(responseContext.hasEntity()).thenReturn(true);
        // TODO: Write a real JerseyTest to avoid this "cheating"
        when(responseContext.getEntityClass()).thenReturn((Class) Map.class);
        when(responseContext.getEntity()).thenReturn(ImmutableMap.of("foo", "bar"));
        final Method resourceMethod = TestResource.class.getMethod("captureResponseEntity", int.class);
        when(resourceInfo.getResourceMethod()).thenReturn(resourceMethod);

        filter.filter(requestContext, responseContext);

        final Map<String, Object> expectedContext = ImmutableMap.of("remote_address", "127.0.0.1");
        verify(auditLogger, only()).success("subject", "read", "object", expectedContext);
    }

    private class TestResource {
        @GET
        public String getWithoutAnnotation() {
            return "foobar";
        }

        @GET
        @AuditLog(subject = "subject", object = "object")
        public String overrideSubject() {
            return "foobar";
        }

        @GET
        @AuditLog(action = "action", object = "object")
        public String detectSubject() {
            return "foobar";
        }

        @GET
        @AuditLog(subject = "subject", action = "foobar", object = "object")
        public String overrideAction() {
            return "foobar";
        }

        @GET
        @AuditLog(subject = "subject", object = "object", captureRequestContext = false)
        public String captureRequestContextDisabled(int id) {
            return "foobar";
        }

        @GET
        @AuditLog(subject = "subject", object = "object", captureResponseEntity = true)
        public Map<String, String> captureResponseEntity(int id) {
            return ImmutableMap.of("foo", "bar");
        }

        @POST
        @AuditLog(subject = "subject", object = "object", captureRequestEntity = true)
        public void captureRequestEntity(Map<String, Object> in) {
        }

        @HEAD
        @AuditLog(subject = "subject", object = "object")
        public String head(int id) {
            return "foobar";
        }

        @GET
        @AuditLog(subject = "subject", object = "object")
        public String get(int id) {
            return "foobar";
        }

        @POST
        @AuditLog(subject = "subject", object = "object")
        public void post(int id) {
        }

        @PUT
        @AuditLog(subject = "subject", object = "object")
        public void put(int id) {
        }

        @DELETE
        @AuditLog(subject = "subject", object = "object")
        public void delete(int id) {
        }
    }
}
