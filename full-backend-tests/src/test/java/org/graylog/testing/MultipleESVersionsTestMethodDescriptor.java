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
