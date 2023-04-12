package org.graylog.testing.fullbackend;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.shared.rest.CSPResponseFilter;
import org.hamcrest.Matchers;

import static io.restassured.RestAssured.given;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, withMailServerEnabled = true)
public class FiltersIT {
    private final GraylogApis api;

    public FiltersIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void cspDocumentationBrowser() {
        given()
                .spec(api.requestSpecification())
                .when()
                .get("/api-browser")
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.equalTo("style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-eval' 'unsafe-inline'; img-src 'self' data:;"));
    }

    @ContainerMatrixTest
    void cspWebInterfaceAssets() {
        given()
                .spec(api.requestSpecification())
                .basePath("/")
                .when()
                .get()
                .then()
                .statusCode(200)
                .assertThat().header(CSPResponseFilter.CSP_HEADER,
                        Matchers.equalTo("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-eval'; img-src 'self' data:;"));
    }
}
