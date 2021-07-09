package org.graylog.testing;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;

public interface ESVersionTest {
    void setEsVersion(GraylogBackend backend, RequestSpecification specification);
}
