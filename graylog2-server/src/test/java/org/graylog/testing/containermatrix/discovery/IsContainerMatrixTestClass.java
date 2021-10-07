package org.graylog.testing.containermatrix.discovery;

import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.engine.discovery.predicates.IsContainerMatrixTest;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsPotentialTestContainer;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.function.Predicate;

public class IsContainerMatrixTestClass extends IsTestClassWithTests {
    private static final IsContainerMatrixTest isTestMethod = new IsContainerMatrixTest();

    private static final IsPotentialTestContainer isPotentialTestContainer = new IsPotentialTestContainer();

    private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

    private boolean hasNestedTests(Class<?> candidate) {
        return !ReflectionUtils.findNestedClasses(candidate, isNestedTestClass).isEmpty();
    }

    public static final Predicate<Method> isTestOrTestFactoryOrTestTemplateMethod = isTestMethod;

    @Override
    public boolean test(Class<?> candidate) {
        boolean isContainer = isPotentialTestContainer.test(candidate);
        boolean isAnnotated = AnnotationSupport.isAnnotated(candidate, ContainerMatrixTestsConfiguration.class);
        boolean hasTestMethod = hasTestOrTestFactoryOrTestTemplateMethods(candidate);
        return isContainer && isAnnotated && (hasTestMethod || hasNestedTests(candidate));
    }

    private boolean hasTestOrTestFactoryOrTestTemplateMethods(Class<?> candidate) {
        return ReflectionUtils.isMethodPresent(candidate, isTestOrTestFactoryOrTestTemplateMethod);
    }

}
