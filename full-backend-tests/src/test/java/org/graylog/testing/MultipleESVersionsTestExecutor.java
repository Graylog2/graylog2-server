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

import com.google.common.io.Resources;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.ElasticsearchInstanceFactory;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.MultipleESVersionsTest;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.opentest4j.AssertionFailedError;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.restassured.http.ContentType.JSON;

public class MultipleESVersionsTestExecutor {
    private boolean skipPackaging = false;

    public void execute(ExecutionRequest request, TestDescriptor descriptor) {
        if (descriptor instanceof EngineDescriptor)
            executeContainer(request, descriptor);
        if (descriptor instanceof MultipleESVersionsTestClassDescriptor)
            executeMethods(((MultipleESVersionsTestClassDescriptor)descriptor).getEsVersion(), request, (MultipleESVersionsTestClassDescriptor)descriptor);
    }

    private GraylogBackend constructBackendFrom(String version, Class testClass, boolean skipPackaging) {
        MultipleESVersionsTest annotation = AnnotationSupport
                .findAnnotation(testClass, MultipleESVersionsTest.class)
                .orElseThrow(IllegalArgumentException::new);

        final String[] fixtures = annotation.mongoDBFixtures();
        final List<URL> mongoDBFixtures = Arrays.stream(fixtures).map(resourceName -> {
            if (! Paths.get(resourceName).isAbsolute()) {
                try {
                    return Resources.getResource(testClass, resourceName);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return Resources.getResource(resourceName);
        }).collect(Collectors.toList());

        final ElasticsearchInstanceFactory esInstanceFactory = instantiateFactory(annotation.elasticsearchFactory());
        final List<Path> pluginJars = instantiateFactory(annotation.pluginJarsProvider()).getJars();
        final Path mavenProjectDir = instantiateFactory(annotation.mavenProjectDirProvider()).getProjectDir();
        return GraylogBackend.createStarted(annotation.extraPorts(), Optional.of(version), esInstanceFactory, pluginJars, mavenProjectDir,
                mongoDBFixtures, skipPackaging);
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

    private void executeMethods(String esVersion, ExecutionRequest request, MultipleESVersionsTestClassDescriptor containerDescriptor) {
        request.getEngineExecutionListener().executionStarted(containerDescriptor);

        TestExecutionResult executionResult;

        try {
            ESVersionTest testInstance = (ESVersionTest)ReflectionUtils.newInstance(containerDescriptor.getTestClass());

            GraylogBackend backend = constructBackendFrom(esVersion, containerDescriptor.getTestClass(), this.skipPackaging);

            // skip packaging after first time
            this.skipPackaging = true;

            RequestSpecification specification = requestSpec(backend);

            testInstance.setEsVersion(backend, specification);

            for (TestDescriptor descriptor : containerDescriptor.getChildren()) {
                executeMethod(testInstance, request, (MultipleESVersionsTestMethodDescriptor)descriptor);
            }

            backend.close();

            executionResult = TestExecutionResult.successful();
        } catch (Throwable throwable) {
            String message = String.format( //
                    "Cannot create instance of class '%s'. Maybe it has no default constructor?", //
                    containerDescriptor.getTestClass() //
            );
            executionResult = TestExecutionResult.failed(new RuntimeException(message, throwable));
        }
        request.getEngineExecutionListener().executionFinished(containerDescriptor, executionResult);
    }

    private void executeMethod(Object testInstance, ExecutionRequest request, MultipleESVersionsTestMethodDescriptor methodTestDescriptor) {
        request.getEngineExecutionListener().executionStarted(methodTestDescriptor);
        TestExecutionResult executionResult = invokeTestMethod(methodTestDescriptor, testInstance);
        request.getEngineExecutionListener().executionFinished(methodTestDescriptor, executionResult);
    }

    private TestExecutionResult invokeTestMethod(MultipleESVersionsTestMethodDescriptor methodTestDescriptor, Object testInstance) {
        try {

            boolean success = (boolean) ReflectionUtils.invokeMethod(methodTestDescriptor.getTestMethod(), testInstance);
            if (success)
                return TestExecutionResult.successful();
            else {
                String message = String.format( //
                        "Test '%s' failed for instance '%s'", //
                        methodTestDescriptor.getDisplayName(), //
                        testInstance.toString() //
                );
                return TestExecutionResult.failed(new AssertionFailedError(message));
            }
        } catch (Throwable throwable) {
            return TestExecutionResult.failed(throwable);
        }
    }

    private void executeContainer(ExecutionRequest request, TestDescriptor containerDescriptor) {
        request.getEngineExecutionListener().executionStarted(containerDescriptor);
        for (TestDescriptor descriptor : containerDescriptor.getChildren()) {
            execute(request, descriptor);
        }
        request.getEngineExecutionListener().executionFinished(containerDescriptor, TestExecutionResult.successful());
    }
}
