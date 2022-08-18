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

import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistrar;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

public class ContainerMatrixTestClassDescriptor extends ClassBasedTestDescriptor {
    public static final String SEGMENT_TYPE = "class";

    private final SearchVersion esVersion;
    private final MongodbServer mongoVersion;
    private final List<URL> mongoFixtures;
    private final boolean preImportLicense;

    public ContainerMatrixTestClassDescriptor(TestDescriptor parent, Class<?> testClass,
                                              JupiterConfiguration configuration,
                                              @Nullable SearchVersion esVersion,
                                              @Nullable MongodbServer mongoVersion,
                                              List<URL> mongoFixtures,
                                              boolean preImportLicense) {
        super(
                parent.getUniqueId().append(SEGMENT_TYPE, testClass.getName() + "_" + esVersion + "_" + mongoVersion),
                testClass,
                determineDisplayName(testClass, esVersion, mongoVersion),
                configuration
        );
        this.esVersion = esVersion;
        this.mongoVersion = mongoVersion;
        this.mongoFixtures = mongoFixtures;
        this.preImportLicense = preImportLicense;
        setParent(parent);
    }

    public ContainerMatrixTestClassDescriptor(TestDescriptor parent,
                                              Class<?> testClass,
                                              JupiterConfiguration configuration,
                                              List<URL> mongoFixtures,
                                              boolean preImportLicense) {
        this(parent, testClass, configuration, null, null, mongoFixtures, preImportLicense);
    }

    private static Supplier<String> determineDisplayName(Class<?> testClass, @Nullable SearchVersion esVersion, @Nullable MongodbServer mongodbServer) {
        final String searchVersionName = Optional.ofNullable(esVersion).map(SearchVersion::toString).orElse("UNKNOWN");
        final String mongodbVersionName = Optional.ofNullable(mongodbServer).map(MongodbServer::getVersion).orElse("UNKNOWN");
        return () -> SEGMENT_TYPE + " " + testClass.getSimpleName().replaceAll("_", " ") + " Search " + searchVersionName + ", MongoDB " + mongodbVersionName;
    }

    // --- TestDescriptor ------------------------------------------------------

    @Override
    public Set<TestTag> getTags() {
        // return modifiable copy
        return new LinkedHashSet<>(this.tags);
    }

    @Override
    public List<Class<?>> getEnclosingTestClasses() {
        return emptyList();
    }

    // --- Node ----------------------------------------------------------------

    @Override
    protected TestInstances instantiateTestClass(JupiterEngineExecutionContext parentExecutionContext,
                                                 ExtensionRegistry registry, ExtensionRegistrar registrar, ExtensionContext extensionContext,
                                                 ThrowableCollector throwableCollector) {
        return instantiateTestClass(Optional.empty(), registry, extensionContext);
    }

    @Override
    public JupiterEngineExecutionContext prepare(JupiterEngineExecutionContext context) {
        return super.prepare(context);
    }

    public SearchVersion getEsVersion() {
        return esVersion;
    }

    public MongodbServer getMongoVersion() {
        return mongoVersion;
    }

    public List<URL> getMongoFixtures() {
        return mongoFixtures;
    }

    public boolean isPreImportLicense() {
        return preImportLicense;
    }
}
