package org.junit.jupiter.engine.discovery.predicates;

import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;

public class IsContainerMatrixTest extends IsTestableMethod {
    public IsContainerMatrixTest() {
        super(ContainerMatrixTest.class, true);
    }
}
