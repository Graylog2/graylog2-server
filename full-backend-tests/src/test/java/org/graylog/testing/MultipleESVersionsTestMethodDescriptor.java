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
package org.graylog.testing;

import org.junit.platform.engine.support.descriptor.*;

import java.lang.reflect.*;

public class MultipleESVersionsTestMethodDescriptor extends AbstractTestDescriptor {
    private final Method testMethod;
    private final Class testClass;

    public MultipleESVersionsTestMethodDescriptor(Method testMethod, Class testClass, MultipleESVersionsTestClassDescriptor parent) {
        super( //
                parent.getUniqueId().append("method", testMethod.getName()), //
                determineDisplayName(testMethod), //
                MethodSource.from(testMethod) //
        );
        this.testMethod = testMethod;
        this.testClass = testClass;
        setParent(parent);
    }

    private static String determineDisplayName(Method testMethod) {
        return DisplayName.canonize(testMethod.getName());
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
