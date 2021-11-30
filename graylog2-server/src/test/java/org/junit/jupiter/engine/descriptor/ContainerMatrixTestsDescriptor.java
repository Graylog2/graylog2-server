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

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ContainerMatrixTestsDescriptor extends AbstractTestDescriptor {
    public static final String SEGMENT_TYPE = "matrix";

    private final Lifecycle lifecycle;
    private final Class<? extends MavenProjectDirProvider> mavenProjectDirProvider;
    private final Class<? extends PluginJarsProvider> pluginJarsProvider;
    private final String esVersion;
    private final String mongoVersion;
    private final Set<Integer> extraPorts = Collections.synchronizedSet(new HashSet<>());
    private final Set<URL> mongoDBFixtures = Collections.synchronizedSet(new HashSet<>());

    public ContainerMatrixTestsDescriptor(TestDescriptor parent,
                                          Lifecycle lifecycle,
                                          Class<? extends MavenProjectDirProvider> mavenProjectDirProvider,
                                          String mavenProjectDirProviderId,
                                          Class<? extends PluginJarsProvider> pluginJarsProvider,
                                          String pluginJarsProviderId,
                                          String esVersion,
                                          String mongoVersion,
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

    private static String createKey(Lifecycle lifecycle, String mavenProjectDirProvider, String pluginJarsProvider, String esVersion, String mongoVersion) {
        return "Lifecyle: " + lifecycle.name() + ", MavenProjectDirProvider: " + mavenProjectDirProvider + ", PluginJarsProvider: " + pluginJarsProvider + ", Elasticsearch: " + esVersion + ", MongoDB: " + mongoVersion;
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

    public String getEsVersion() {
        return esVersion;
    }

    public String getMongoVersion() {
        return mongoVersion;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public void addExtraPorts(final int[] ports) {
        Arrays.stream(ports).forEach(e -> extraPorts.add(e));
    }

    public int[] getExtraPorts() {
        return extraPorts.stream().mapToInt(i -> i).toArray();
    }

    public List<URL> getMongoDBFixtures() {
        return mongoDBFixtures.stream().collect(Collectors.toList());
    }
}
