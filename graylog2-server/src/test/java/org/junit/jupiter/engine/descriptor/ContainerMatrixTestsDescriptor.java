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

import org.graylog.testing.completebackend.DefaultMavenProjectDirProvider;
import org.graylog.testing.completebackend.DefaultPluginJarsProvider;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog2.storage.SearchVersion;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContainerMatrixTestsDescriptor extends AbstractTestDescriptor {
    public static final String SEGMENT_TYPE = "matrix";

    private final Lifecycle lifecycle;
    private final Class<? extends MavenProjectDirProvider> mavenProjectDirProvider;
    private final Class<? extends PluginJarsProvider> pluginJarsProvider;
    private final SearchVersion esVersion;
    private final MongodbServer mongoVersion;
    private final Set<Integer> extraPorts = Collections.synchronizedSet(new HashSet<>());
    private final Set<URL> mongoDBFixtures = Collections.synchronizedSet(new HashSet<>());

    public ContainerMatrixTestsDescriptor(TestDescriptor parent,
                                          Lifecycle lifecycle,
                                          Class<? extends MavenProjectDirProvider> mavenProjectDirProvider,
                                          String mavenProjectDirProviderId,
                                          Class<? extends PluginJarsProvider> pluginJarsProvider,
                                          String pluginJarsProviderId,
                                          SearchVersion esVersion,
                                          MongodbServer mongoVersion,
                                          Set<Integer> extraPorts,
                                          List<URL> mongoDBFixtures) {
        super(parent.getUniqueId().append(SEGMENT_TYPE,
                        createKey(lifecycle, mavenProjectDirProviderId, pluginJarsProviderId, esVersion, mongoVersion)),
                createKey(lifecycle, mavenProjectDirProviderId, pluginJarsProviderId, esVersion, mongoVersion));
        setParent(parent);
        this.lifecycle = lifecycle;
        this.mavenProjectDirProvider = mavenProjectDirProvider;
        this.pluginJarsProvider = pluginJarsProvider;
        this.esVersion = esVersion;
        this.mongoVersion = mongoVersion;
        this.extraPorts.addAll(extraPorts);
        this.mongoDBFixtures.addAll(mongoDBFixtures);
    }

    public ContainerMatrixTestsDescriptor(TestDescriptor parent,
                                          String displayName,
                                          Set<Integer> extraPorts,
                                          List<URL> mongoDBFixtures) {
        super(parent.getUniqueId().append(SEGMENT_TYPE,
                        displayName),
                displayName);
        setParent(parent);
        this.lifecycle = Lifecycle.VM;
        this.mavenProjectDirProvider = DefaultMavenProjectDirProvider.class;
        this.pluginJarsProvider = DefaultPluginJarsProvider.class;
        this.esVersion = SearchServer.DEFAULT_VERSION.getSearchVersion();
        this.mongoVersion = MongodbServer.DEFAULT_VERSION;
        this.extraPorts.addAll(extraPorts);
        this.mongoDBFixtures.addAll(mongoDBFixtures);
    }

    protected static String createKey(Lifecycle lifecycle, String mavenProjectDirProvider, String pluginJarsProvider, SearchVersion esVersion, MongodbServer mongoVersion) {
        return "Lifecycle: " + lifecycle.name() + ", MavenProjectDirProvider: " + mavenProjectDirProvider + ", PluginJarsProvider: " + pluginJarsProvider + ", Search: " + esVersion + ", MongoDB: " + mongoVersion.getVersion();
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

    public SearchVersion getEsVersion() {
        return esVersion;
    }

    public MongodbServer getMongoVersion() {
        return mongoVersion;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public void addExtraPorts(final int[] ports) {
        Arrays.stream(ports).forEach(extraPorts::add);
    }

    public int[] getExtraPorts() {
        return extraPorts.stream().mapToInt(i -> i).toArray();
    }

    public List<URL> getMongoDBFixtures() {
        return new ArrayList<>(mongoDBFixtures);
    }
}
