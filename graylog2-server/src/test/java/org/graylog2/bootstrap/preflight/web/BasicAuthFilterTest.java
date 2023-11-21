package org.graylog2.bootstrap.preflight.web;

import org.apache.commons.codec.digest.DigestUtils;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.server.ContainerRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class BasicAuthFilterTest {

    @Test
    void testInvalidCredentials() throws IOException {
        final BasicAuthFilter filter = new BasicAuthFilter("admin", DigestUtils.sha256Hex("admin"), "junit-test");
        final ContainerRequest request = mockRequest("admin", "admin1");
        filter.filter(request);
        final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(request, times(1)).abortWith(captor.capture());
        final Response response = captor.getValue();
        Assertions.assertThat(response.getStatusInfo().getStatusCode()).isEqualTo(401);
        Assertions.assertThat(response.getStatusInfo().getReasonPhrase()).isEqualTo("Unauthorized");
        Assertions.assertThat(response.getEntity()).isEqualTo("You cannot access this resource, invalid username/password combination!");
    }

    @Test
    void testMissingPassword() throws IOException {
        final BasicAuthFilter filter = new BasicAuthFilter("admin", DigestUtils.sha256Hex("admin"), "junit-test");
        final ContainerRequest request = mockRequest("admin", "");
        filter.filter(request);
        final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        Mockito.verify(request, times(1)).abortWith(captor.capture());
        final Response response = captor.getValue();
        Assertions.assertThat(response.getStatusInfo().getStatusCode()).isEqualTo(401);
        Assertions.assertThat(response.getStatusInfo().getReasonPhrase()).isEqualTo("Unauthorized");
        Assertions.assertThat(response.getEntity()).isEqualTo("You cannot access this resource, invalid username/password combination!");
    }

    @Test
    void testCorrectCredentials() throws IOException {
        final BasicAuthFilter filter = new BasicAuthFilter("admin", DigestUtils.sha256Hex("admin"), "junit-test");
        final ContainerRequest request = mockRequest("admin", "admin");
        filter.filter(request);
        Mockito.verify(request, Mockito.never()).abortWith(Mockito.any());
    }

    private static ContainerRequest mockRequest(String username, String password) {
        final ContainerRequest request = mock(ContainerRequest.class);
        final MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
        headers.put("Authorization", Collections.singletonList("Basic " + base64(username, password)));
        Mockito.when(request.getHeaders()).thenReturn(headers);
        return request;
    }

    @NotNull
    private static String base64(String username, String password) {
        final String value = username + ":" + password;
        return new String(Base64.getEncoder().encode(value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
}
