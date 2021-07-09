package org.graylog.testing;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.*;
import org.junit.platform.engine.*;
import org.junit.platform.engine.support.descriptor.*;

import java.lang.reflect.*;
import java.util.function.*;

class MultipleESVersionsTestClassDescriptor extends AbstractTestDescriptor {
    private final Class<?> testClass;
    private final String esVersion;

    public MultipleESVersionsTestClassDescriptor(Class<?> testClass, TestDescriptor parent, String esVersion) {
        super( //
                parent.getUniqueId().append("class", testClass.getName() + "_" + esVersion), //
                determineDisplayName(testClass) + " " + esVersion, //
                ClassSource.from(testClass) //
        );
        this.esVersion = esVersion;
        this.testClass = testClass;
        setParent(parent);
        addAllChildren();
    }

    private static String determineDisplayName(Class testClass) {
        return DisplayName.canonize(testClass.getSimpleName());
    }

    private void addAllChildren() {
        Predicate<Method> isTestMethod = method -> {
            if(AnnotationSupport.isAnnotated(method, Test.class))
                return true;
            if (ReflectionUtils.isStatic(method))
                return false;
            if (ReflectionUtils.isPrivate(method))
                return false;
            if (ReflectionUtils.isAbstract(method))
                return false;
            if (method.getParameterCount() > 0)
                return false;
            return method.getReturnType().equals(boolean.class) || method.getReturnType().equals(Boolean.class);
        };
        ReflectionUtils.findMethods(testClass, isTestMethod).stream() //
                .map(method -> new MultipleESVersionsTestMethodDescriptor(method, testClass, this)) //
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
}
