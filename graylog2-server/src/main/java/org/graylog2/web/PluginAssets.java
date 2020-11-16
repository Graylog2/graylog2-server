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
package org.graylog2.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Plugin;
import org.graylog2.shared.plugins.PluginLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginAssets {
    private static final Logger LOG = LoggerFactory.getLogger(PluginAssets.class);

    public static final String pathPrefix = "web-interface/assets";
    private static final String pluginPathPrefix = "plugin/";
    private static final String manifestFilename = "module.json";
    private static final String vendorManifestFilename = "vendor-module.json";

    private final ObjectMapper objectMapper;
    private final List<String> jsFiles;
    private final List<String> cssFiles;
    private final List<String> vendorJsFiles;

    @Inject
    public PluginAssets(Set<Plugin> plugins,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jsFiles = new ArrayList<>();
        this.cssFiles = new ArrayList<>();

        final InputStream vendorManifestStream = this.getClass().getResourceAsStream("/" + pathPrefix + "/" + vendorManifestFilename);
        if (vendorManifestStream != null) {
            final ModuleManifest vendorManifest;
            try {
                vendorManifest = objectMapper.readValue(vendorManifestStream, ModuleManifest.class);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read vendor manifest: ", e);
            }
            this.vendorJsFiles = vendorManifest.files().jsFiles();
            jsFiles.addAll(vendorManifest.files().jsFiles());
            cssFiles.addAll(vendorManifest.files().cssFiles());
        } else {
            throw new IllegalStateException("Unable to find vendor assets. Maybe the web interface was not built into server?");
        }

        plugins.forEach(plugin -> {
            final ModuleManifest pluginManifest = manifestForPlugin(plugin);
            final String pathPrefix = pluginPathPrefix + plugin.metadata().getUniqueId() + "/";
            if (pluginManifest != null) {
                jsFiles.addAll(prefixFileNames(pluginManifest.files().jsFiles(), pathPrefix));
                cssFiles.addAll(prefixFileNames(pluginManifest.files().cssFiles(), pathPrefix));
            }
        });

        final InputStream packageManifestStream = this.getClass().getResourceAsStream("/" + pathPrefix + "/" + manifestFilename);
        if (packageManifestStream != null) {
            final ModuleManifest manifest;
            try {
                manifest = objectMapper.readValue(packageManifestStream, ModuleManifest.class);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read web interface manifest: ", e);
            }
            jsFiles.addAll(manifest.files().jsFiles());
            cssFiles.addAll(manifest.files().cssFiles());
        } else {
            throw new IllegalStateException("Unable to find web interface assets. Maybe the web interface was not built into server?");
        }
    }

    private List<String> jsFiles() {
        return jsFiles;
    }

    // Sort JS files in the intended load order, so templates don't need to care about it.
    List<String> sortedJsFiles() {
        return jsFiles().stream()
                .sorted((file1, file2) -> {
                    // Vendor JS scripts go first
                    if (vendorJsFiles.contains(file1)) {
                        return -1;
                    }
                    if (vendorJsFiles.contains(file2)) {
                        return 1;
                    }
                    // Polyfill JS script goes second
                    if (file1.startsWith("polyfill")) {
                        return -1;
                    }

                    if (file2.startsWith("polyfill")) {
                        return 1;
                    }
                    // Builtins JS script goes third
                    if (file1.startsWith("builtins")) {
                        return -1;
                    }

                    if (file2.startsWith("builtins")) {
                        return 1;
                    }

                    // App JS script goes last, as plugins need to be loaded before
                    return file2.compareTo(file1);
                })
                .collect(Collectors.toList());
    }

    List<String> cssFiles() {
        return cssFiles;
    }

    private List<String> prefixFileNames(List<String> filenames, String pathPrefix) {
        return filenames.stream().map(file -> file.startsWith(pathPrefix) ? file : pathPrefix + file).collect(Collectors.toList());
    }

    @Nullable
    private ModuleManifest manifestForPlugin(Plugin plugin) {
        if (!(plugin instanceof PluginLoader.PluginAdapter)) {
            LOG.warn("Unable to read web manifest from plugin " + plugin + ": Plugin is not an instance of PluginAdapter.");
            return null;
        }

        final String pluginClassName = ((PluginLoader.PluginAdapter) plugin).getPluginClassName();
        final InputStream manifestStream = plugin.metadata().getClass().getResourceAsStream("/plugin." + pluginClassName + "." + manifestFilename);
        if (manifestStream != null) {
            try {
                return objectMapper.readValue(manifestStream, ModuleManifest.class);
            } catch (IOException e) {
                LOG.warn("Unable to read web manifest from plugin " + plugin + ": ", e);
            }
        }

        LOG.debug("No valid web manifest found for plugin " + plugin);

        return null;
    }
}
