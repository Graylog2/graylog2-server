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
package org.graylog2.web.resources;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ContainerRequest;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;
import org.graylog2.web.IndexHtmlGenerator;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.activation.MimetypesFileTypeMap;
import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebInterfaceAssetsResourceTest {
    private static final String PLUGIN_ID = "org.graylog.plugins.test.TestPlugin";

    private IndexHtmlGenerator indexHtmlGenerator;
    private HttpHeaders headers;
    private WebInterfaceAssetsResource resource;

    /**
     * A real class rather than a mock, so that it has a meaningful code source (this module's test classes)
     * for {@link ResourceFileReader#readFileFromPlugin(String, Class)} to check assets against.
     */
    private static class TestPluginMetaData implements PluginMetaData {
        @Override
        public String getUniqueId() {
            return PLUGIN_ID;
        }

        @Override
        public String getName() {
            return "Test";
        }

        @Override
        public String getAuthor() {
            return "Graylog, Inc.";
        }

        @Override
        public URI getURL() {
            return URI.create("https://www.graylog.org/");
        }

        @Override
        public Version getVersion() {
            return Version.from(1, 0, 0);
        }

        @Override
        public String getDescription() {
            return "Test plugin";
        }

        @Override
        public Version getRequiredVersion() {
            return Version.from(1, 0, 0);
        }

        @Override
        public Set<ServerStatus.Capability> getRequiredCapabilities() {
            return Set.of();
        }
    }

    @BeforeEach
    void setUp() {
        indexHtmlGenerator = mock(IndexHtmlGenerator.class);
        when(indexHtmlGenerator.get(any(), any())).thenReturn("<html>index</html>");

        headers = mock(HttpHeaders.class);
        when(headers.getRequestHeaders()).thenReturn(new MultivaluedHashMap<>());

        final HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getHttpExternalUri()).thenReturn(URI.create("http://localhost:9000/"));

        final Plugin plugin = mock(Plugin.class);
        when(plugin.metadata()).thenReturn(new TestPluginMetaData());

        resource = new WebInterfaceAssetsResource(indexHtmlGenerator,
                Set.of(plugin),
                new MimetypesFileTypeMap(),
                httpConfiguration,
                mock(CustomizationConfig.class),
                new ResourceFileReader());
    }

    @Test
    void servesAnAssetOfTheRequestedPlugin() {
        final Response response = resource.get(mock(ContainerRequest.class), headers, PLUGIN_ID, "plugin-asset-test.js");

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat((byte[]) response.getEntity()).isNotEmpty();
    }

    /**
     * Plugin assets are looked up at the root of the plugin JAR, so a traversal sequence normalizes away and
     * lands on another JAR's root-level resource. {@code log4j2.xml} ships in the server's classes directory,
     * so it belongs to a different code source than this plugin.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "../../log4j2.xml",
            "../log4j2.xml",
            "log4j2.xml",
            "..",
            "/etc/passwd",
            "org/graylog2/web/resources/outside.txt",
    })
    void refusesToServePluginAssetsThatAreNotThePluginsOwn(String filename) {
        assertThatThrownBy(() -> resource.get(mock(ContainerRequest.class), headers, PLUGIN_ID, filename))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void doesNotReflectTheRequestedPluginAssetNameBackToTheClient() {
        assertThatThrownBy(() -> resource.get(mock(ContainerRequest.class), headers, PLUGIN_ID, "../log4j2.xml"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageNotContaining("log4j2.xml");
    }

    /**
     * The non-plugin asset route deliberately falls back to the single-page-application entry point for
     * anything it cannot resolve, so a rejected traversal is indistinguishable from any other unknown route.
     * This pins that behaviour so the fallback cannot turn into a file read.
     */
    @ParameterizedTest
    @ValueSource(strings = {"../../log4j2.xml", "..", "/etc/passwd", "../outside.txt"})
    void servesTheIndexPageInsteadOfTraversingOutOfTheAssetsDirectory(String filename) {
        final Response response = resource.get(mock(ContainerRequest.class), headers, filename);

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_HTML);
        assertThat(response.getEntity()).isEqualTo("<html>index</html>");
        verify(indexHtmlGenerator).get(any(), any());
    }
}
