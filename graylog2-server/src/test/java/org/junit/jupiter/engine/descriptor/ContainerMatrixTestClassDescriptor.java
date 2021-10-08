package org.junit.jupiter.engine.descriptor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.jupiter.engine.extension.ExtensionRegistrar;
import org.junit.jupiter.engine.extension.ExtensionRegistry;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.hierarchical.ThrowableCollector;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

public class ContainerMatrixTestClassDescriptor extends ClassBasedTestDescriptor {
    public static final String SEGMENT_TYPE = "class";

    private final String esVersion;
    private final String mongoVersion;

    public ContainerMatrixTestClassDescriptor(TestDescriptor parent, Class<?> testClass, JupiterConfiguration configuration, String esVersion, String mongoVersion) {
        super(
                parent.getUniqueId().append(SEGMENT_TYPE, testClass.getName() + "_" + esVersion + "_" + mongoVersion),
                testClass,
                determineDisplayName(testClass, esVersion, mongoVersion),
                configuration
        );
        this.esVersion = esVersion;
        this.mongoVersion = mongoVersion;
        setParent(parent);
    }

    private static Supplier<String> determineDisplayName(Class testClass, String esVersion, String mongoVersion) {
        return () -> SEGMENT_TYPE + " " + testClass.getSimpleName().replaceAll("_", " ") + " Elasticsearch " + esVersion + ", MongoDB " + mongoVersion;
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

    public String getEsVersion() {
        return esVersion;
    }

    public String getMongoVersion() {
        return mongoVersion;
    }
}
