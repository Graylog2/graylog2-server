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

import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static org.graylog.testing.containermatrix.ContainerMatrixTestEngine.evaluate;

public class ContainerMatrixTestClassDescriptor extends AbstractTestDescriptor {
    private final Class<?> testClass;
    private final String esVersion;
    private final String mongoVersion;

    private boolean first = false;
    private boolean last = false;

    public ContainerMatrixTestClassDescriptor(Class<?> testClass, TestDescriptor parent, String esVersion, String mongoVersion) {
        super(
                parent.getUniqueId().append("class", testClass.getName() + "_" + esVersion + "_" + mongoVersion),
                determineDisplayName(testClass) + " Elasticsearch " + esVersion + ", MongoDB " + mongoVersion,
                ClassSource.from(testClass)
        );
        this.esVersion = esVersion;
        this.mongoVersion = mongoVersion;
        this.testClass = testClass;
        setParent(parent);
        addAllChildren();
    }

    private static String determineDisplayName(Class testClass) {
        return testClass.getSimpleName().replaceAll("_", " ");
    }

    private static final Predicate<Method> IS_CANDIDATE = method -> {
        if (AnnotationSupport.isAnnotated(method, ContainerMatrixTest.class)) {
            if (AnnotationSupport.isAnnotated(method, EnabledIfEnvironmentVariable.class)) {
                return AnnotationSupport
                        .findAnnotation(method, EnabledIfEnvironmentVariable.class).map(a -> !evaluate(a).isDisabled())
                        .orElseThrow(() -> new RuntimeException("Annotation should exist - it has been checked for the given class before..."));
            }
            if (AnnotationSupport.isAnnotated(method, DisabledIfEnvironmentVariable.class)) {
                return AnnotationSupport
                        .findAnnotation(method, DisabledIfEnvironmentVariable.class).map(a -> !evaluate(a).isDisabled())
                        .orElseThrow(() -> new RuntimeException("Annotation should exist - it has been checked for the given class before..."));
            }
            return true;
        }
        return false;
    };

    private void addAllChildren() {
        ReflectionUtils.findMethods(testClass, IS_CANDIDATE).stream()
                .map(method -> new ContainerMatrixTestMethodDescriptor(method, testClass, this))
                .forEach(this::addChild);
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    public Class getTestClass() {
        return testClass;
    }

    public String getEsVersion() {
        return esVersion;
    }

    public String getMongoVersion() {
        return mongoVersion;
    }

    public String getKey() {
        return esVersion + "_" + mongoVersion;
    }

    public void setFirst() {
        this.first = true;
    }

    public void setLast() {
        this.last = true;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
