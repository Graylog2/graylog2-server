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
package org.graylog.testing.containermatrix.descriptors;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.MavenProjectDirProvider;
import org.graylog.testing.completebackend.PluginJarsProvider;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.graylog.testing.containermatrix.ContainerVersions.DEFAULT_ES;
import static org.graylog.testing.containermatrix.ContainerVersions.DEFAULT_MONGO;

public class ContainerMatrixTestsDescriptor implements TestDescriptor {
    private final UniqueId uniqueId;
    private final String key;
    private final String displayName;
    private TestDescriptor parent;
    private final TestSource source;
    private final Lifecycle lifecycle;
    private final MavenProjectDirProvider mavenProjectDirProvider;
    private final PluginJarsProvider pluginJarsProvider;

    private final Map<String, Set<TestDescriptor>> children = Collections.synchronizedMap(new HashMap<>());
    private final Set<Integer> extraPorts = Collections.synchronizedSet(emptySet());

    public ContainerMatrixTestsDescriptor(TestDescriptor parent, Lifecycle lifecycle, Class<? extends MavenProjectDirProvider> mavenProjectDirProvider, Class<? extends PluginJarsProvider> pluginJarsProvider) {
        setParent(parent);
        this.source = null;
        this.lifecycle = lifecycle;
        this.mavenProjectDirProvider = instantiateFactory(mavenProjectDirProvider);
        this.pluginJarsProvider = instantiateFactory(pluginJarsProvider);
        this.key = createKey(lifecycle, this.mavenProjectDirProvider, this.pluginJarsProvider);
        this.uniqueId = parent.getUniqueId().append("container_1", key);
        this.displayName = "ContainerMatrixTestsDescriptor: " + this.uniqueId;
    }

    private <T> T instantiateFactory(Class<? extends T> providerClass) {
        try {
            return providerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct instance of " + providerClass.getSimpleName() + ": ", e);
        }
    }

    private String createKey(Lifecycle lifecycle, MavenProjectDirProvider mavenProjectDirProvider, PluginJarsProvider pluginJarsProvider) {
        return lifecycle.name() + "_" + mavenProjectDirProvider.getUniqueId() + "_" + pluginJarsProvider.getUniqueId();
    }

    public boolean is(ContainerMatrixTestsConfiguration annotation) {
        MavenProjectDirProvider mavenProjectDirProvider = instantiateFactory(annotation.mavenProjectDirProvider());
        PluginJarsProvider pluginJarsProvider = instantiateFactory(annotation.pluginJarsProvider());

        return this.key.equals(createKey(annotation.serverLifecycle(), mavenProjectDirProvider, pluginJarsProvider));
    }

    @Override
    public final UniqueId getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public final String getDisplayName() {
        return this.displayName;
    }

    @Override
    public Set<TestTag> getTags() {
        return emptySet();
    }

    @Override
    public Optional<TestSource> getSource() {
        return Optional.ofNullable(this.source);
    }

    @Override
    public final Optional<TestDescriptor> getParent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public final void setParent(TestDescriptor parent) {
        this.parent = parent;
    }

    @Override
    public final Set<? extends TestDescriptor> getChildren() {
        Set<? extends TestDescriptor> set = this.children.entrySet().stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(set);
    }

    public final Map<String, Set<? extends TestDescriptor>> getChildrenMatrix() {
        return Collections.unmodifiableMap(this.children);
    }

    private void addChild(final String key, final TestDescriptor child) {
        if (children.containsKey(key)) {
            children.get(key).add(child);
        } else {
            children.put(key, Stream.of(child).collect(Collectors.toCollection(LinkedHashSet::new)));
        }
    }

    private String getDefaultKey() {
        return DEFAULT_ES + "_" + DEFAULT_MONGO;
    }

    @Override
    public void addChild(TestDescriptor child) {
        Preconditions.notNull(child, "child must not be null");
        child.setParent(this);
        if (child instanceof ContainerMatrixTestClassDescriptor) {
            ContainerMatrixTestClassDescriptor descriptor = (ContainerMatrixTestClassDescriptor) child;
            addChild(descriptor.getKey(), descriptor);
        } else {
            addChild(getDefaultKey(), child);
        }
    }

    @Override
    public void removeChild(final TestDescriptor child) {
        Preconditions.notNull(child, "child must not be null");
        this.children.entrySet().forEach(c -> c.getValue().remove(child));
        child.setParent(null);
    }

    @Override
    public void removeFromHierarchy() {
        Preconditions.condition(!isRoot(), "cannot remove the root of a hierarchy");
        this.parent.removeChild(this);
        this.getChildren().forEach(child -> child.setParent(null));
        this.children.clear();
    }

    @Override
    public Optional<? extends TestDescriptor> findByUniqueId(final UniqueId uniqueId) {
        Preconditions.notNull(uniqueId, "UniqueId must not be null");
        if (getUniqueId().equals(uniqueId)) {
            return Optional.of(this);
        }
        // @formatter:off
        return this.getChildren().stream()
                .map(child -> child.findByUniqueId(uniqueId))
                .filter(Optional::isPresent)
                .findAny()
                .orElse(Optional.empty());
        // @formatter:on
    }

    @Override
    public final int hashCode() {
        return this.uniqueId.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        TestDescriptor that = (TestDescriptor) other;
        return this.getUniqueId().equals(that.getUniqueId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + getUniqueId();
    }

    public String getInfo() {
        return "TODO: create info";
    }

    public MavenProjectDirProvider getMavenProjectDirProvider() {
        return mavenProjectDirProvider;
    }

    public PluginJarsProvider getPluginJarsProvider() {
        return pluginJarsProvider;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public void addExtraPorts(final int[] ports) {
        Arrays.stream(ports).forEach(e -> extraPorts.add(e));
    }
}
