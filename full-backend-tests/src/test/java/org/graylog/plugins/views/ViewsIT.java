package org.graylog.plugins.views;

import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

import static org.hamcrest.core.StringContains.containsString;

@ContainerMatrixTestsConfiguration
public class ViewsIT {
    private final GraylogApis api;

    public ViewsIT(GraylogApis apis) {
        this.api = apis;
    }

    @BeforeAll
    public void importMongoFixtures() {
        this.api.backend().importMongoDBFixture("mongodb-stored-views-for-issue15086.json", ViewsIT.class);
    }

    @ContainerMatrixTest
    void testIssue15086Dashboard() {
        api.get("/views/63e0f94c17263921e7fefeb3", Map.of(), 200)
                .assertThat().body(containsString("org.graylog2.decorators.FormatStringDecorator"));
    }

    @ContainerMatrixTest
    void testIssue15086Search() {
        api.get("/views/643e60873c985e899977ba1c", Map.of(), 200)
                .assertThat().body(containsString("org.graylog2.decorators.FormatStringDecorator"));
    }
}
