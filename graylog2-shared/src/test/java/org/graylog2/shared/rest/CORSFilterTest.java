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
package org.graylog2.shared.rest;

import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CORSFilterTest {
    @Test
    public void testFilterWithOrigin() throws Exception {
        final MultivaluedHashMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        requestHeaders.add("Origin", "example.com");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(requestHeaders);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedHashMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        final CORSFilter corsFilter = new CORSFilter();

        corsFilter.filter(requestContext, responseContext);

        assertThat(responseHeaders.getFirst("Access-Control-Allow-Origin")).isEqualTo("example.com");
        assertThat(responseHeaders.getFirst("Access-Control-Allow-Credentials")).isEqualTo(true);
        assertThat((String) responseHeaders.getFirst("Access-Control-Allow-Headers"))
                .contains("Authorization", "Content-Type", "X-Graylog2-No-Session-Extension");
        assertThat((String) responseHeaders.getFirst("Access-Control-Allow-Methods"))
                .contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    public void testFilterWithoutOrigin() throws Exception {
        final MultivaluedHashMap<String, String> requestHeaders = new MultivaluedHashMap<>();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        when(requestContext.getHeaders()).thenReturn(requestHeaders);
        final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
        final MultivaluedHashMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        final CORSFilter corsFilter = new CORSFilter();

        corsFilter.filter(requestContext, responseContext);

        assertThat(responseHeaders).isEmpty();
    }
}