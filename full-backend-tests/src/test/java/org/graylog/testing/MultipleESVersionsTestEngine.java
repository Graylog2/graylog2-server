package org.graylog.testing;

import org.graylog.testing.completebackend.MultipleESVersionsTest;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Predicate;

public class MultipleESVersionsTestEngine implements TestEngine {
    private static final String ENGINE_ID = "graylog-multiple-es";

    private static final Predicate<Class<?>> IS_MULTIPLE_ES_CONTAINER = classCandidate -> {
        if (ReflectionUtils.isAbstract(classCandidate))
            return false;
        if (ReflectionUtils.isPrivate(classCandidate))
            return false;
        return AnnotationSupport.isAnnotated(classCandidate, MultipleESVersionsTest.class);
    };

    @Override
    public String getId() {
        return ENGINE_ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        TestDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "Graylog Multiple ES Versions Test");

        request.getSelectorsByType(PackageSelector.class).forEach(selector -> {
            appendTestsInPackage(selector.getPackageName(), engineDescriptor);
        });

        request.getSelectorsByType(ClassSelector.class).forEach(selector -> {
            appendTestsInClass(selector.getJavaClass(), engineDescriptor);
        });

        return engineDescriptor;
    }

    private void appendTestsInClass(Class<?> javaClass, TestDescriptor engineDescriptor) {
        if (IS_MULTIPLE_ES_CONTAINER.test(javaClass)) {
            Optional<MultipleESVersionsTest> annotation = AnnotationSupport.findAnnotation(javaClass, MultipleESVersionsTest.class);
            if(annotation.isPresent()) {
                // convert to set to make sure that versions are unique and not listed multiple times
                new HashSet<>(Arrays.asList(annotation.get().esVersions())).forEach(version ->
                        engineDescriptor.addChild(new MultipleESVersionsTestClassDescriptor(javaClass, engineDescriptor, version))
                     );
            }
        }
    }

    private void appendTestsInPackage(String packageName, TestDescriptor engineDescriptor) {
        ReflectionSupport.findAllClassesInPackage(packageName, IS_MULTIPLE_ES_CONTAINER, name -> true)
                .stream()
                .forEach(aClass -> appendTestsInClass(aClass, engineDescriptor));

    }

    @Override
    public void execute(ExecutionRequest request) {
        TestDescriptor root = request.getRootTestDescriptor();
        new MultipleESVersionsTestExecutor().execute(request, root);
    }
}
