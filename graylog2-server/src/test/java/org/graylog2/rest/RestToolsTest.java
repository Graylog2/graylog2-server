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
package org.graylog2.rest;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestToolsTest {
    @Test
    public void buildEndpointUriReturnsDefaultUriIfHeaderIsMissing() throws Exception {
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getRequestHeader(anyString())).thenReturn(ImmutableList.of());
        final URI endpointUri = URI.create("http://graylog.example.com");
        assertThat(RestTools.buildEndpointUri(httpHeaders, endpointUri)).isEqualTo(endpointUri.toString());
    }

    @Test
    public void buildEndpointUriReturnsDefaultUriIfHeaderIsEmpty() throws Exception {
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getRequestHeader(anyString())).thenReturn(ImmutableList.of(""));
        final URI endpointUri = URI.create("http://graylog.example.com");
        assertThat(RestTools.buildEndpointUri(httpHeaders, endpointUri)).isEqualTo(endpointUri.toString());
    }

    @Test
    public void buildEndpointUriReturnsHeaderValueIfHeaderIsPresent() throws Exception {
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getRequestHeader(anyString())).thenReturn(ImmutableList.of("http://header.example.com"));
        final URI endpointUri = URI.create("http://graylog.example.com");
        assertThat(RestTools.buildEndpointUri(httpHeaders, endpointUri)).isEqualTo("http://header.example.com");
    }

    @Test
    public void buildEndpointUriReturnsFirstHeaderValueIfMultipleHeadersArePresent() throws Exception {
        final HttpHeaders httpHeaders = mock(HttpHeaders.class);
        when(httpHeaders.getRequestHeader(anyString())).thenReturn(
            ImmutableList.of("http://header1.example.com", "http://header2.example.com"));
        final URI endpointUri = URI.create("http://graylog.example.com");
        assertThat(RestTools.buildEndpointUri(httpHeaders, endpointUri)).isEqualTo("http://header1.example.com");
    }
}
