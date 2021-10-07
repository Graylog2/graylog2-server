package org.junit.jupiter.engine.discovery;

import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestsDescriptor;
import org.graylog.testing.containermatrix.discovery.IsContainerMatrixTestClass;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixEngineDescriptor;
import org.junit.jupiter.engine.descriptor.ContainerMatrixTestClassDescriptor;
import org.junit.jupiter.engine.descriptor.NestedClassTestDescriptor;
import org.junit.jupiter.engine.discovery.predicates.IsNestedTestClass;
import org.junit.jupiter.engine.discovery.predicates.IsTestClassWithTests;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toCollection;
import static org.graylog.testing.containermatrix.discovery.IsContainerMatrixTestClass.isTestOrTestFactoryOrTestTemplateMethod;
import static org.junit.platform.commons.support.ReflectionSupport.findNestedClasses;
import static org.junit.platform.commons.util.FunctionUtils.where;
import static org.junit.platform.commons.util.ReflectionUtils.findMethods;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.engine.support.discovery.SelectorResolver.Resolution.unresolved;

public class ContainerMatrixClassSelectorResolver implements SelectorResolver {

    private static final IsTestClassWithTests isTestClassWithTests = new IsContainerMatrixTestClass();
    private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();

    private final Predicate<String> classNameFilter;
    private final JupiterConfiguration configuration;

    ContainerMatrixClassSelectorResolver(Predicate<String> classNameFilter, JupiterConfiguration configuration) {
        this.classNameFilter = classNameFilter;
        this.configuration = configuration;
    }

    @Override
    public Resolution resolve(ClassSelector selector, Context context) {
        Class<?> testClass = selector.getJavaClass();
        if (isTestClassWithTests.test(testClass)) {
            // Nested tests are never filtered out
            if (classNameFilter.test(testClass.getName())) {
                return toResolution(
                        context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass))));
            }
        } else if (isNestedTestClass.test(testClass)) {
            return toResolution(context.addToParent(() -> DiscoverySelectors.selectClass(testClass.getEnclosingClass()),
                    parent -> Optional.of(newNestedClassTestDescriptor(parent, testClass))));
        }
        return unresolved();
    }

    @Override
    public Resolution resolve(NestedClassSelector selector, Context context) {
        if (isNestedTestClass.test(selector.getNestedClass())) {
            return toResolution(context.addToParent(() -> selectClass(selector.getEnclosingClasses()),
                    parent -> Optional.of(newNestedClassTestDescriptor(parent, selector.getNestedClass()))));
        }
        return unresolved();
    }

    @Override
    public Resolution resolve(UniqueIdSelector selector, Context context) {
        UniqueId uniqueId = selector.getUniqueId();
        UniqueId.Segment lastSegment = uniqueId.getLastSegment();
        if (ContainerMatrixTestClassDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
            String className = lastSegment.getValue();
            return ReflectionUtils.tryToLoadClass(className).toOptional().filter(isTestClassWithTests).map(
                    testClass -> toResolution(
                            context.addToParent(parent -> Optional.of(newClassTestDescriptor(parent, testClass))))).orElse(
                    unresolved());
        }
        if (NestedClassTestDescriptor.SEGMENT_TYPE.equals(lastSegment.getType())) {
            String simpleClassName = lastSegment.getValue();
            return toResolution(context.addToParent(() -> selectUniqueId(uniqueId.removeLastSegment()), parent -> {
                if (parent instanceof ClassBasedTestDescriptor) {
                    Class<?> parentTestClass = ((ClassBasedTestDescriptor) parent).getTestClass();
                    return ReflectionUtils.findNestedClasses(parentTestClass,
                            isNestedTestClass.and(
                                    where(Class::getSimpleName, isEqual(simpleClassName)))).stream().findFirst().flatMap(
                            testClass -> Optional.of(newNestedClassTestDescriptor(parent, testClass)));
                }
                return Optional.empty();
            }));
        }
        return unresolved();
    }

    private ContainerMatrixTestsDescriptor createNewDescriptor(ContainerMatrixEngineDescriptor engineDescriptor, ContainerMatrixTestsConfiguration configuration) {
        ContainerMatrixTestsDescriptor containerMatrixTestsDescriptor = new ContainerMatrixTestsDescriptor(engineDescriptor, configuration.serverLifecycle(), configuration.mavenProjectDirProvider(), configuration.pluginJarsProvider());
        engineDescriptor.addChild(containerMatrixTestsDescriptor);
        return containerMatrixTestsDescriptor;
    }

    private ContainerMatrixTestsDescriptor findOrCreateDescriptor(final TestDescriptor engineDescriptor, final ContainerMatrixTestsConfiguration configuration) {
        // for the given configuration, reuse/add to an existing descriptor
        return ((ContainerMatrixEngineDescriptor) engineDescriptor)
                .getChildren()
                .stream()
                .filter(child -> child instanceof ContainerMatrixTestsDescriptor)
                .map(child -> (ContainerMatrixTestsDescriptor) child)
                .filter(child -> child.is(configuration))
                .findFirst()
                .orElse(createNewDescriptor((ContainerMatrixEngineDescriptor) engineDescriptor, configuration));
    }

    private TestDescriptor getRoot(TestDescriptor descriptor) {
        if (descriptor.isRoot()) {
            return descriptor;
        }
        return getRoot(descriptor.getParent().get());
    }

    private TestDescriptor newClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
        // find Annotation
        ContainerMatrixTestsConfiguration annotation = AnnotationSupport
                .findAnnotation(testClass, ContainerMatrixTestsConfiguration.class)
                .orElseThrow(() -> new RuntimeException("Annotation should exist - it has been checked for the given class before..."));
        // convert to set to make sure that versions are unique and not listed multiple times, use LinkedHashSet to preserve order
        Set<String> esVersions = new LinkedHashSet<>(Arrays.asList(annotation.esVersions()));
        Set<String> mongoVersions = new LinkedHashSet<>(Arrays.asList(annotation.mongoVersions()));


        // find/create new parent
        // TODO: maintain old hierarchy instead of flattening
        ContainerMatrixTestsDescriptor containerMatrixTestsDescriptor = findOrCreateDescriptor(getRoot(parent), annotation);
        containerMatrixTestsDescriptor.addExtraPorts(annotation.extraPorts());
        esVersions.forEach(esVersion -> mongoVersions.forEach(mongoVersion ->
                containerMatrixTestsDescriptor.addChild(new ContainerMatrixTestClassDescriptor(parent, testClass, configuration, esVersion, mongoVersion))
        ));

        // TODO: fix this
        return containerMatrixTestsDescriptor;
    }

    private NestedClassTestDescriptor newNestedClassTestDescriptor(TestDescriptor parent, Class<?> testClass) {
        return new NestedClassTestDescriptor(
                parent.getUniqueId().append(NestedClassTestDescriptor.SEGMENT_TYPE, testClass.getSimpleName()), testClass,
                configuration);
    }

    private Resolution toResolution(Optional<? extends ClassBasedTestDescriptor> testDescriptor) {
        return testDescriptor.map(it -> {
            Class<?> testClass = it.getTestClass();
            List<Class<?>> testClasses = new ArrayList<>(it.getEnclosingTestClasses());
            testClasses.add(testClass);
            // @formatter:off
            return Resolution.match(Match.exact(it, () -> {
                Stream<DiscoverySelector> methods = findMethods(testClass, isTestOrTestFactoryOrTestTemplateMethod).stream()
                        .map(method -> selectMethod(testClasses, method));
                Stream<NestedClassSelector> nestedClasses = findNestedClasses(testClass, isNestedTestClass).stream()
                        .map(nestedClass -> DiscoverySelectors.selectNestedClass(testClasses, nestedClass));
                return Stream.concat(methods, nestedClasses).collect(toCollection((Supplier<Set<DiscoverySelector>>) LinkedHashSet::new));
            }));
            // @formatter:on
        }).orElse(unresolved());
    }

    private DiscoverySelector selectClass(List<Class<?>> classes) {
        if (classes.size() == 1) {
            return DiscoverySelectors.selectClass(classes.get(0));
        }
        int lastIndex = classes.size() - 1;
        return DiscoverySelectors.selectNestedClass(classes.subList(0, lastIndex), classes.get(lastIndex));
    }

    private DiscoverySelector selectMethod(List<Class<?>> classes, Method method) {
        if (classes.size() == 1) {
            return DiscoverySelectors.selectMethod(classes.get(0), method);
        }
        int lastIndex = classes.size() - 1;
        return DiscoverySelectors.selectNestedMethod(classes.subList(0, lastIndex), classes.get(lastIndex), method);
    }

}
