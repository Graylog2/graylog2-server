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
package org.junit.jupiter.engine.descriptor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.graylog.testing.completebackend.DefaultMavenProjectDirProvider;
import org.graylog.testing.completebackend.DefaultPluginJarsProvider;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.storage.SearchVersion;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog.testing.containermatrix.ContainerMatrixTestEngine.getSearchVersionOverride;
import static org.graylog.testing.containermatrix.ContainerMatrixTestEngine.isCompatible;

public class ContainerMatrixTestsDescriptor extends AbstractTestDescriptor {
    public static final String SEGMENT_TYPE = "matrix";

    private final Lifecycle lifecycle;
    private final Class<? extends MavenProjectDirProvider> mavenProjectDirProvider;
    private final Class<? extends PluginJarsProvider> pluginJarsProvider;
    private final SearchVersion searchVersion;
    private final MongodbServer mongoVersion;
    private final Set<URL> mongoDBFixtures = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> enabledFeatureFlags = Collections.synchronizedSet(new HashSet<>());
    private final boolean withMailServerEnabled;
    private final boolean withWebhookServerEnabled;
    private final Map<String, String> additionalConfigurationParameters;

    public ContainerMatrixTestsDescriptor(TestDescriptor parent,
                                          Lifecycle lifecycle,
                                          Class<? extends MavenProjectDirProvider> mavenProjectDirProvider,
                                          String mavenProjectDirProviderId,
                                          Class<? extends PluginJarsProvider> pluginJarsProvider,
                                          String pluginJarsProviderId,
                                          SearchVersion esVersion,
                                          MongodbServer mongoVersion,
                                          List<URL> mongoDBFixtures,
                                          List<String> enabledFeatureFlags, boolean withMailServerEnabled, boolean withWebhookServerEnabled, Map<String, String> additionalConfigurationParameters) {
        super(
                parent.getUniqueId().append(SEGMENT_TYPE, createKey(lifecycle, mavenProjectDirProviderId, pluginJarsProviderId, esVersion, mongoVersion, withMailServerEnabled, withWebhookServerEnabled, additionalConfigurationParameters, enabledFeatureFlags)),
                createKey(lifecycle, mavenProjectDirProviderId, pluginJarsProviderId, esVersion, mongoVersion, withMailServerEnabled, withWebhookServerEnabled, additionalConfigurationParameters, enabledFeatureFlags)
        );
        setParent(parent);
        this.lifecycle = lifecycle;
        this.mavenProjectDirProvider = mavenProjectDirProvider;
        this.pluginJarsProvider = pluginJarsProvider;
        this.searchVersion = esVersion;
        this.mongoVersion = mongoVersion;
        this.mongoDBFixtures.addAll(mongoDBFixtures);
        this.enabledFeatureFlags.addAll(enabledFeatureFlags);
        this.withMailServerEnabled = withMailServerEnabled;
        this.withWebhookServerEnabled = withWebhookServerEnabled;
        this.additionalConfigurationParameters = additionalConfigurationParameters;
    }

    public ContainerMatrixTestsDescriptor(TestDescriptor parent,
                                          String displayName,
                                          List<URL> mongoDBFixtures) {
        super(parent.getUniqueId().append(SEGMENT_TYPE,
                        displayName),
                displayName);
        setParent(parent);
        this.lifecycle = Lifecycle.VM;
        this.mavenProjectDirProvider = DefaultMavenProjectDirProvider.class;
        this.pluginJarsProvider = DefaultPluginJarsProvider.class;
        this.searchVersion = SearchServer.DEFAULT_VERSION.getSearchVersion();
        this.mongoVersion = MongodbServer.DEFAULT_VERSION;
        this.mongoDBFixtures.addAll(mongoDBFixtures);
        this.withMailServerEnabled = false;
        this.withWebhookServerEnabled = false;
        this.additionalConfigurationParameters = new HashMap<>();
    }

    protected static String createKey(Lifecycle lifecycle, String mavenProjectDirProvider, String pluginJarsProvider, SearchVersion searchVersion,
                                      MongodbServer mongoVersion, boolean withMailServerEnabled, boolean withWebhookServerEnabled, Map<String, String> additionalConfigurationParameters, List<String> enabledFeatureFlags) {
        final ImmutableMap.Builder<String, Object> values = ImmutableMap.<String, Object>builder()
                .put("Lifecycle", lifecycle.name())
                .put("MavenProjectDirProvider", mavenProjectDirProvider)
                .put("PluginJarsProvider", pluginJarsProvider)
                .put("Search", searchVersion)
                .put("MongoDB", mongoVersion.getVersion());

        if(withMailServerEnabled) {
            values.put("Mailserver", "enabled");
        }

        if(withWebhookServerEnabled) {
            values.put("Webhookserver", "enabled");
        }

        values.putAll(additionalConfigurationParameters);

        if(!enabledFeatureFlags.isEmpty()) {
            values.put("Featureflags", enabledFeatureFlags.stream().collect(Collectors.joining(",")));
        }

        return values.build().entrySet().stream().map(pair -> String.format(Locale.ROOT, "%s: %s", pair.getKey(), pair.getValue()))
                .collect(Collectors.joining(", "));
    }

    public Class<? extends MavenProjectDirProvider> getMavenProjectDirProvider() {
        return mavenProjectDirProvider;
    }

    public Class<? extends PluginJarsProvider> getPluginJarsProvider() {
        return pluginJarsProvider;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public SearchVersion getSearchVersion() {
        return searchVersion;
    }

    public MongodbServer getMongoVersion() {
        return mongoVersion;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public List<URL> getMongoDBFixtures() {
        return new ArrayList<>(mongoDBFixtures);
    }

    public List<String> getEnabledFeatureFlags() {
        return new ArrayList<>(enabledFeatureFlags);
    }

    public boolean withEnabledMailServer() {
        return withMailServerEnabled;
    }

    public Map<String, String> getAdditionalConfigurationParameters() {
        return additionalConfigurationParameters;
    }

    public boolean withEnabledWebhookServer() {
        return withWebhookServerEnabled;
    }

    public boolean matches(ContainerMatrixTestsConfiguration config) {
        return config.serverLifecycle().equals(this.getLifecycle())
                && config.mavenProjectDirProvider().equals(this.getMavenProjectDirProvider())
                && config.pluginJarsProvider().equals(this.getPluginJarsProvider())
                && isMatchingSearchServer(config)
                && getMongodbServers(config).contains(this.getMongoVersion())
                && config.withMailServerEnabled() == this.withEnabledMailServer()
                && config.withWebhookServerEnabled() == this.withEnabledWebhookServer()
                && featureFlagsMatching(config)
                && paramsMatching(config);
    }

    private boolean paramsMatching(ContainerMatrixTestsConfiguration config) {
        final Map<String, String> params = Arrays.stream(config.additionalConfigurationParameters())
                .collect(Collectors.toMap(ContainerMatrixTestsConfiguration.ConfigurationParameter::key, ContainerMatrixTestsConfiguration.ConfigurationParameter::value));
        final Map<String, String> containerParams = this.getAdditionalConfigurationParameters();
        return containerParams.size() == params.size() && containerParams.entrySet().stream().allMatch(entry -> Objects.equals(params.get(entry.getKey()), entry.getValue()));
    }

    private boolean featureFlagsMatching(ContainerMatrixTestsConfiguration config) {
        final List<String> configFlags = Arrays.stream(config.enabledFeatureFlags()).toList();
        final List<String> containerFlags = this.getEnabledFeatureFlags();
        return containerFlags.size() == configFlags.size() && containerFlags.containsAll(configFlags);
    }


    private static  Set<MongodbServer> getMongodbServers(ContainerMatrixTestsConfiguration config) {
        return Sets.newHashSet(config.mongoVersions());
    }

    private boolean isMatchingSearchServer(ContainerMatrixTestsConfiguration config) {
        final var optional = getSearchVersionOverride();
        if(optional.isPresent()) {
            if(config.searchVersions().length == 0) {
                return true;
            } else {
                final var override = optional.get();
                return Arrays.stream(config.searchVersions()).anyMatch(version -> isCompatible(override, version));
            }
        } else {
            return getSearchServers(config).contains(this.getSearchVersion());
        }
    }

    private static Set<SearchVersion> getSearchServers(ContainerMatrixTestsConfiguration config) {
        return Stream.of(config.searchVersions()).map(SearchServer::getSearchVersion).collect(Collectors.toSet());
    }
}
