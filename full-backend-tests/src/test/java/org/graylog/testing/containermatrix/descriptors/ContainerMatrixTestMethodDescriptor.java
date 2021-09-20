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

import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;

import java.lang.reflect.Method;

public class ContainerMatrixTestMethodDescriptor extends AbstractTestDescriptor {
    private final Method testMethod;
    private final Class testClass;

    public ContainerMatrixTestMethodDescriptor(Method testMethod, Class testClass, ContainerMatrixTestClassDescriptor parent) {
        super(
                parent.getUniqueId().append("method", testMethod.getName()),
                determineDisplayName(testMethod),
                MethodSource.from(testMethod)
        );
        this.testMethod = testMethod;
        this.testClass = testClass;
        setParent(parent);
    }

    private static String determineDisplayName(Method testMethod) {
        return testMethod.getName().replaceAll("_", " ");
    }

    public Method getTestMethod() {
        return testMethod;
    }

    public Class getTestClass() {
        return testClass;
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }

}
