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
package org.graylog.testing.containermatrix;

import com.google.common.io.Resources;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.graylog.storage.ElasticSearchInstanceFactoryByVersion;
import org.graylog.testing.completebackend.ElasticsearchInstanceFactory;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.annotations.AfterVersion;
import org.graylog.testing.containermatrix.annotations.BeforeVersion;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixEngineDescriptor;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestClassDescriptor;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestMethodDescriptor;
import org.graylog.testing.containermatrix.descriptors.ContainerMatrixTestsDescriptor;
import org.graylog.testing.graylognode.MavenPackager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static io.restassured.http.ContentType.JSON;
import static org.graylog.testing.graylognode.ExecutableFileUtil.makeSureExecutableIsFound;

public class ContainerMatrixTestExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ContainerMatrixTestExecutor.class);

    private int[] extraPorts = {};

    public void execute(ExecutionRequest request, TestDescriptor descriptor) {
        if (descriptor instanceof ContainerMatrixTestClassDescriptor) {
            executeMethods(((ContainerMatrixTestClassDescriptor) descriptor).getEsVersion(), ((ContainerMatrixTestClassDescriptor) descriptor).getMongoVersion(), extraPorts, request, (ContainerMatrixTestClassDescriptor) descriptor);
        } else if (descriptor instanceof ContainerMatrixEngineDescriptor) {
            executeContainer(request, (ContainerMatrixEngineDescriptor) descriptor);
        } else if (descriptor instanceof ContainerMatrixTestsDescriptor) {
            executeContainerMatrix(request, (ContainerMatrixTestsDescriptor) descriptor);
        } else {
            LOG.warn("Unknown TestDescriptor in this context: " + descriptor.getClass().getSimpleName());
        }
    }

    private GraylogBackend constructBackendFrom(String esVersion, String mongoVersion, int[] extraPorts, Class testClass) {
        ContainerMatrixTestsConfiguration annotation = AnnotationSupport
                .findAnnotation(testClass, ContainerMatrixTestsConfiguration.class)
                .orElseThrow(IllegalArgumentException::new);

        final String[] fixtures = annotation.mongoDBFixtures();
        final List<URL> mongoDBFixtures = Arrays.stream(fixtures).map(resourceName -> {
            if (!Paths.get(resourceName).isAbsolute()) {
                try {
                    return Resources.getResource(testClass, resourceName);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return Resources.getResource(resourceName);
        }).collect(Collectors.toList());

        final ElasticsearchInstanceFactory esInstanceFactory = instantiateFactory(ElasticSearchInstanceFactoryByVersion.class);
        final List<Path> pluginJars = instantiateFactory(annotation.pluginJarsProvider()).getJars();
        return GraylogBackend.createStarted(extraPorts, esVersion, mongoVersion, esInstanceFactory, pluginJars, null,
                mongoDBFixtures);
    }

    private <T> T instantiateFactory(Class<? extends T> providerClass) {
        try {
            return providerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to construct instance of " + providerClass.getSimpleName() + ": ", e);
        }
    }

    private RequestSpecification requestSpec(GraylogBackend backend) {
        return new RequestSpecBuilder().build()
                .baseUri(backend.uri())
                .port(backend.apiPort())
                .basePath("/api")
                .accept(JSON)
                .contentType(JSON)
                .header("X-Requested-By", "peterchen")
                .auth().basic("admin", "admin");
    }

    private void runAnnotatedMethods(Object instance, Class<? extends Annotation> annotation) {
        List<Method> methods = AnnotationSupport.findAnnotatedMethods(instance.getClass(), annotation, HierarchyTraversalMode.TOP_DOWN);
        methods.stream().forEach(method -> ReflectionUtils.invokeMethod(method, instance));

    }

    // TODO: also be able to instantiate classes with only one ore no args (or different order)
    private Object createNewInstance(Class clazz, GraylogBackend backend, RequestSpecification specification) {
        final Constructor constructor = ReflectionUtils.findConstructors(clazz, c -> {
            Class<?>[] parameterTypes = c.getParameterTypes();
            return (parameterTypes[0].equals(GraylogBackend.class) && parameterTypes[1].equals(RequestSpecification.class));
        }).stream().findFirst().orElseThrow(() -> new RuntimeException("Could not find suitable constructor."));
        return ReflectionUtils.newInstance(constructor, backend, specification);
    }

    private void executeMethods(String esVersion, String mongoVersion, int[] extraPorts, ExecutionRequest request, ContainerMatrixTestClassDescriptor containerDescriptor) {
        request.getEngineExecutionListener().executionStarted(containerDescriptor);

        TestExecutionResult executionResult;

        try {
            GraylogBackend backend = constructBackendFrom(esVersion, mongoVersion, extraPorts, containerDescriptor.getTestClass());
            RequestSpecification specification = requestSpec(backend);
            final Object testInstance = createNewInstance(containerDescriptor.getTestClass(), backend, specification);

            if (containerDescriptor.isFirst()) {
                runAnnotatedMethods(testInstance, BeforeVersion.class);
            }

            runAnnotatedMethods(testInstance, BeforeAll.class);
            for (TestDescriptor descriptor : containerDescriptor.getChildren()) {
                runAnnotatedMethods(testInstance, BeforeEach.class);
                executeMethod(testInstance, request, (ContainerMatrixTestMethodDescriptor) descriptor);
                runAnnotatedMethods(testInstance, AfterEach.class);
            }
            runAnnotatedMethods(testInstance, AfterAll.class);

            if (containerDescriptor.isLast()) {
                runAnnotatedMethods(testInstance, AfterVersion.class);
            }

            backend.close();

            executionResult = TestExecutionResult.successful();
        } catch (Throwable throwable) {
            String message = String.format(Locale.getDefault(),
                    "Cannot create instance of class '%s'. Maybe it has no constructor with (GraylogBackend, RequestSpecification)?",
                    containerDescriptor.getTestClass()
            );
            executionResult = TestExecutionResult.failed(new RuntimeException(message, throwable));
        }
        request.getEngineExecutionListener().executionFinished(containerDescriptor, executionResult);
    }

    private void executeMethod(Object testInstance, ExecutionRequest request, ContainerMatrixTestMethodDescriptor methodTestDescriptor) {
        request.getEngineExecutionListener().executionStarted(methodTestDescriptor);
        TestExecutionResult executionResult = invokeTestMethod(methodTestDescriptor, testInstance);
        request.getEngineExecutionListener().executionFinished(methodTestDescriptor, executionResult);
    }

    private TestExecutionResult invokeTestMethod(ContainerMatrixTestMethodDescriptor methodTestDescriptor, Object testInstance) {
        try {
            ReflectionUtils.invokeMethod(methodTestDescriptor.getTestMethod(), testInstance);
            return TestExecutionResult.successful();
        } catch (AssertionError e) {
            String message = String.format(Locale.getDefault(),
                    "Test '%s' failed for instance '%s'",
                    methodTestDescriptor.getDisplayName(),
                    testInstance.toString()
            );
            return TestExecutionResult.failed(new AssertionFailedError(message));
        } catch (Throwable throwable) {
            return TestExecutionResult.failed(throwable);
        }
    }

    private void executeContainer(ExecutionRequest request, ContainerMatrixEngineDescriptor containerDescriptor) {
        LOG.info("Executing Container Matrix Tests, starting with Engine-Descriptor");

        String lastContainerMatrixMvnCombination = "";

        request.getEngineExecutionListener().executionStarted(containerDescriptor);
        for (TestDescriptor descriptor : containerDescriptor.getChildren()) {
            if (descriptor instanceof ContainerMatrixTestsDescriptor && !lastContainerMatrixMvnCombination.equals(((ContainerMatrixTestsDescriptor) descriptor).getMavenProjectDirProvider().getUniqueId())) {
                if (!MavenPackager.isRunFromMaven()) {
                    makeSureExecutableIsFound("mvn");
                    MavenPackager.packageJar(((ContainerMatrixTestsDescriptor) descriptor).getMavenProjectDirProvider().getProjectDir());
                }
                lastContainerMatrixMvnCombination = ((ContainerMatrixTestsDescriptor) descriptor).getMavenProjectDirProvider().getUniqueId();
            }
            execute(request, descriptor);
        }
        request.getEngineExecutionListener().executionFinished(containerDescriptor, TestExecutionResult.successful());

        LOG.info("Finishing Container Matrix Tests, finished with Engine-Descriptor");
    }

    private void executeContainerMatrix(ExecutionRequest request, ContainerMatrixTestsDescriptor matrixTestsDescriptor) {
        LOG.info("Executing Container Matrix Tests, starting tests for combination: " + matrixTestsDescriptor.getDisplayName());
        LOG.debug(matrixTestsDescriptor.getInfo());

        request.getEngineExecutionListener().executionStarted(matrixTestsDescriptor);

        // mark first test and last test to make sure that the @BeforeVersion or @AfterVersion annotations are run
        matrixTestsDescriptor.getChildren().stream().filter(f -> f instanceof ContainerMatrixTestClassDescriptor).findFirst().ifPresent(test -> ((ContainerMatrixTestClassDescriptor) test).setFirst());
        final long count = matrixTestsDescriptor.getChildren().stream().filter(f -> f instanceof ContainerMatrixTestClassDescriptor).count();
        matrixTestsDescriptor.getChildren().stream().filter(f -> f instanceof ContainerMatrixTestClassDescriptor).skip(count - 1).findFirst().ifPresent(test -> ((ContainerMatrixTestClassDescriptor) test).setLast());

        for (TestDescriptor descriptor : matrixTestsDescriptor.getChildren()) {
            execute(request, descriptor);
        }
        request.getEngineExecutionListener().executionFinished(matrixTestsDescriptor, TestExecutionResult.successful());

        LOG.info("Finished with combination");
    }
}
