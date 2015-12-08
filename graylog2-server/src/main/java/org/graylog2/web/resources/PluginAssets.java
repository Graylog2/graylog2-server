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
package org.graylog2.web.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Plugin;
import org.graylog2.web.ModuleManifest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginAssets {
    private static final Logger LOG = LoggerFactory.getLogger(PluginAssets.class);

    public static String pathPrefix = "web-interface/assets";
    private static String pluginPathPrefix = "/plugin/";
    private static String manifestFilename = "module.json";

    private final ObjectMapper objectMapper;
    private final List<String> jsFiles;
    private final List<String> cssFiles;

    @Inject
    public PluginAssets(Set<Plugin> plugins,
                        ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        this.jsFiles = new ArrayList<>();
        this.cssFiles = new ArrayList<>();

        plugins.stream().forEach(plugin -> {
            final ModuleManifest pluginManifest = manifestForPlugin(plugin);
            final String pathPrefix = pluginPathPrefix + plugin.metadata().getUniqueId() + "/";
            if (pluginManifest != null) {
                jsFiles.addAll(pluginManifest.files().jsFiles().stream().map(file -> file.startsWith(pathPrefix) ? file : pathPrefix + file).collect(Collectors.toList()));
                cssFiles.addAll(pluginManifest.files().cssFiles().stream().map(file -> file.startsWith(pathPrefix) ? file : pathPrefix + file).collect(Collectors.toList()));
            }
        });
        final InputStream packageManifest = this.getClass().getResourceAsStream("/" + pathPrefix + "/" + manifestFilename);
        if (packageManifest != null) {
            final ModuleManifest manifest = objectMapper.readValue(packageManifest, ModuleManifest.class);
            jsFiles.addAll(manifest.files().jsFiles());
            cssFiles.addAll(manifest.files().cssFiles());
        } else {
            LOG.warn("Unable to find web interface assets. Maybe the web interface was not built into server?");
        }
    }

    public List<String> jsFiles() {
        return jsFiles;
    }

    public List<String> cssFiles() {
        return cssFiles;
    }

    private ModuleManifest manifestForPlugin(Plugin plugin) {
        final InputStream manifestStream = plugin.metadata().getClass().getResourceAsStream("/" + manifestFilename);
        if (manifestStream != null) {
            try {
                final ModuleManifest manifest = objectMapper.readValue(manifestStream, ModuleManifest.class);
                return manifest;
            } catch (IOException e) {
                LOG.warn("Unable to read manifest from plugin " + plugin + ": ", e);
            }
        }

        LOG.debug("No valid manifest found for plugin " + plugin);

        return null;
    }
}
