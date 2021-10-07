package org.junit.jupiter.engine.descriptor;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.TestInstances;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.platform.engine.EngineExecutionListener;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;

public class ContainerMatrixExtensionContext extends AbstractExtensionContext<ContainerMatrixEngineDescriptor> {

    ContainerMatrixExtensionContext(EngineExecutionListener engineExecutionListener,
                                    ContainerMatrixEngineDescriptor testDescriptor, JupiterConfiguration configuration) {

        super(null, engineExecutionListener, testDescriptor, configuration);
    }

    @Override
    public Optional<AnnotatedElement> getElement() {
        return Optional.empty();
    }

    @Override
    public Optional<Class<?>> getTestClass() {
        return Optional.empty();
    }

    @Override
    public Optional<TestInstance.Lifecycle> getTestInstanceLifecycle() {
        return Optional.empty();
    }

    @Override
    public Optional<Object> getTestInstance() {
        return Optional.empty();
    }

    @Override
    public Optional<TestInstances> getTestInstances() {
        return Optional.empty();
    }

    @Override
    public Optional<Method> getTestMethod() {
        return Optional.empty();
    }

    @Override
    public Optional<Throwable> getExecutionException() {
        return Optional.empty();
    }
}
