/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.elasticsearch.e2e;

import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.backenddriver.SearchDriver.searchAllMessages;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.junit.Assert.fail;

@ApiIntegrationTest(serverLifecycle = CLASS, extraPorts = {ElasticsearchE2E.GELF_HTTP_PORT})
public class ElasticsearchE2E {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchE2E.class);

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public ElasticsearchE2E(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void inputMessageCanBeSearched() {
        //TODO: dont hardcode port in body
        // create input
        String input = "{\"title\":\"KILL ME\",\"type\":\"org.graylog2.inputs.gelf.http.GELFHttpInput\",\"configuration\":{\"bind_address\":\"0.0.0.0\",\"port\":12201,\"recv_buffer_size\":1048576,\"number_worker_threads\":8,\"tls_cert_file\":\"\",\"tls_key_file\":\"\",\"tls_enable\":false,\"tls_key_password\":\"\",\"tls_client_auth\":\"disabled\",\"tls_client_auth_cert_file\":\"\",\"tcp_keepalive\":false,\"enable_cors\":true,\"max_chunk_size\":65536,\"idle_writer_timeout\":60,\"override_source\":null,\"decompress_size_limit\":8388608},\"global\":true}";

        given()
                .spec(requestSpec)
                .body(input)
                .expect().response().statusCode(201)
                .when()
                .post("/system/inputs");

        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);

        waitForGelfInputListening(mappedPort);

        //post message
        String message = "{\"short_message\":\"Hello there\", \"host\":\"example.org\", \"facility\":\"test\"}";

        gelfEndpoint(mappedPort)
                .body(message)
                .expect().response().statusCode(202)
                .when()
                .post();

        //search message

        wait(5000);

        List<String> messages = searchAllMessages(requestSpec);

        assertThat(messages).containsExactly("Hello there");
    }

    private void waitForGelfInputListening(int port) {
        int timeOutMs = 5000;
        int msPassed = 0;
        int waitMs = 500;
        while (msPassed <= timeOutMs) {
            if (gelfInputIsListening(port)) {
                LOG.info("GELF input listening on port {} after {} ms", port, msPassed);
                return;
            }
            msPassed += waitMs;
            wait(waitMs);
        }
        fail(String.format(Locale.ENGLISH, "Timed out waiting for GELF input listening on port %s after %s ms.", port, msPassed));
    }

    private void wait(int waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean gelfInputIsListening(int mappedPort) {
        try {
            gelfEndpoint(mappedPort)
                    .expect().response().statusCode(200)
                    .when()
                    .options();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private RequestSpecification gelfEndpoint(int mappedPort) {
        return given()
                .spec(requestSpec)
                .basePath("/gelf")
                .port(mappedPort);
    }
}
