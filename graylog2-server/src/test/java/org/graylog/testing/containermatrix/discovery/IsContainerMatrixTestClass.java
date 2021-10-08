package org.graylog.testing.containermatrix.discovery;

import com.google.common.collect.Sets;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestsDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsContainerMatrixTest;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsPotentialTestContainer;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

public class IsContainerMatrixTestClass extends IsTestClassWithTests {
    private static final IsContainerMatrixTest isTestMethod = new IsContainerMatrixTest();

    private static final IsPotentialTestContainer isPotentialTestContainer = new IsPotentialTestContainer();

    private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

    private boolean hasNestedTests(Class<?> candidate) {
        return !ReflectionUtils.findNestedClasses(candidate, isNestedTestClass).isEmpty();
    }

    public static final Predicate<Method> isTestOrTestFactoryOrTestTemplateMethod = isTestMethod;

    private final ContainerMatrixTestsDescriptor container;

    public IsContainerMatrixTestClass(final ContainerMatrixTestsDescriptor container) {
        this.container = container;
    }

    private boolean matchAnnotationToContainer(Class<?> candidate) {
        Optional<ContainerMatrixTestsConfiguration> annotation = AnnotationSupport.findAnnotation(candidate, ContainerMatrixTestsConfiguration.class);
        if (annotation.isPresent()) {
            ContainerMatrixTestsConfiguration config = annotation.get();
            return config.serverLifecycle().equals(container.getLifecycle())
                    && config.mavenProjectDirProvider().equals(container.getMavenProjectDirProvider())
                    && config.pluginJarsProvider().equals(container.getPluginJarsProvider())
                    && Sets.newHashSet(config.esVersions()).contains(container.getEsVersion())
                    && Sets.newHashSet(config.mongoVersions()).contains(container.getMongoVersion());
        } else {
            // Annotation should be present!
            return false;
        }
    }

    @Override
    public boolean test(Class<?> candidate) {
        if (AnnotationSupport.isAnnotated(candidate, ContainerMatrixTestsConfiguration.class)) {
            boolean annotationMatchesContext = matchAnnotationToContainer(candidate);
            boolean isContainer = isPotentialTestContainer.test(candidate);
            boolean hasTestMethod = hasTestOrTestFactoryOrTestTemplateMethods(candidate);
            return isContainer && annotationMatchesContext && (hasTestMethod || hasNestedTests(candidate));
        } else {
            return false;
        }
    }

    private boolean hasTestOrTestFactoryOrTestTemplateMethods(Class<?> candidate) {
        return ReflectionUtils.isMethodPresent(candidate, isTestOrTestFactoryOrTestTemplateMethod);
    }

}
