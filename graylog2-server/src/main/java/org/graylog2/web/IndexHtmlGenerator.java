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
import org.graylog2.Configuration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Singleton
public class IndexHtmlGenerator {
    private static final String title = "Graylog Web Interface";
    private final String content;

    @Inject
    public IndexHtmlGenerator(PluginAssets pluginAssets, Configuration configuration, Engine templateEngine) throws IOException {
        final URL templateUrl = this.getClass().getResource("/web-interface/index.html.template");
        final String template = Resources.toString(templateUrl, StandardCharsets.UTF_8);
        final Map<String, Object> model = ImmutableMap.<String, Object>builder()
                .put("title", title)
                .put("cssFiles", pluginAssets.cssFiles())
                .put("jsFiles", pluginAssets.sortedJsFiles())
                .put("appPrefix", configuration.getWebPrefix())
                .build();

        this.content = requireNonNull(templateEngine, "templateEngine").transform(template, model);
    }

    public String get() {
        return this.content;
    }
}
