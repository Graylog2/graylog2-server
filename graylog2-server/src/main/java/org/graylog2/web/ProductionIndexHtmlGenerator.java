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
package org.graylog2.web;

import com.floreysoft.jmte.Engine;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.RestTools;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Production implementation of the {@link IndexHtmlGenerator} interface that provides an "index.html" page
 * including the production web interface assets.
 *
 * This implementation throws an error when the web interface assets cannot be found in the classpath.
 */
@Singleton
public class ProductionIndexHtmlGenerator implements IndexHtmlGenerator {
    private final String template;
    private final List<String> cssFiles;
    private final List<String> sortedJsFiles;
    private final Engine templateEngine;
    private final HttpConfiguration httpConfiguration;

    @Inject
    public ProductionIndexHtmlGenerator(final PluginAssets pluginAssets,
                                        final Engine templateEngine,
                                        final HttpConfiguration httpConfiguration) throws IOException {
        this(
                Resources.toString(Resources.getResource("web-interface/index.html.template"), StandardCharsets.UTF_8),
                pluginAssets.cssFiles(),
                pluginAssets.sortedJsFiles(),
                templateEngine,
                httpConfiguration);
    }

    private ProductionIndexHtmlGenerator(final String template,
                                         final List<String> cssFiles,
                                         final List<String> sortedJsFiles,
                                         final Engine templateEngine,
                                         final HttpConfiguration httpConfiguration) throws IOException {
        this.template = requireNonNull(template, "template");
        this.cssFiles = requireNonNull(cssFiles, "cssFiles");
        this.sortedJsFiles = requireNonNull(sortedJsFiles, "sortedJsFiles");
        this.templateEngine = requireNonNull(templateEngine, "templateEngine");
        this.httpConfiguration = requireNonNull(httpConfiguration, "httpConfiguration");
    }

    @Override
    public String get(MultivaluedMap<String, String> headers) {
        final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                .put("title", "Graylog Web Interface")
                .put("cssFiles", cssFiles)
                .put("jsFiles", sortedJsFiles)
                .put("appPrefix", RestTools.buildExternalUri(headers, httpConfiguration.getHttpExternalUri()))
                .build();
        return templateEngine.transform(template, model);
    }
}
