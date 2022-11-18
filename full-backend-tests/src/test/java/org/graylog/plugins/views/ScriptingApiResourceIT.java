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
package org.graylog.plugins.views;

import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.plugins.views.search.rest.scriptingapi.ScriptingApiModule;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.GelfInputUtils;
import org.graylog.testing.utils.IndexSetUtils;
import org.graylog.testing.utils.SharingRequest;
import org.graylog.testing.utils.SharingUtils;
import org.graylog.testing.utils.StreamUtils;
import org.graylog.testing.utils.UserUtils;
import org.graylog2.plugin.streams.StreamRuleType;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;

@ContainerMatrixTestsConfiguration(serverLifecycle = CLASS, mongoVersions = MongodbServer.MONGO5,
                                   searchVersions = {SearchServer.ES7, SearchServer.OS2},
                                   enabledFeatureFlags = ScriptingApiModule.FEATURE_FLAG)
public class ScriptingApiResourceIT {

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    private String stream1Id;
    private String stream2Id;

    public ScriptingApiResourceIT(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @BeforeAll
    public void beforeAll() {

        final String defaultIndexSetId = IndexSetUtils.defaultIndexSetId(requestSpec);

        final JsonPath user = UserUtils.createUser(requestSpec, new UserUtils.User(
                "john.doe",
                "asdfgh",
                "John",
                "Doe",
                "john.doe@example.com",
                false,
                30_000,
                "Europe/Vienna",
                Collections.emptyList(),
                Collections.emptyList()
        ));

        final String userId = user.getString("id");


        this.stream1Id = StreamUtils.createStream(requestSpec, "Stream #1", defaultIndexSetId, new StreamUtils.StreamRule(StreamRuleType.EXACT.toInteger(), "stream1", "target_stream", false));
        this.stream2Id = StreamUtils.createStream(requestSpec, "Stream #2", defaultIndexSetId, new StreamUtils.StreamRule(StreamRuleType.EXACT.toInteger(), "stream2", "target_stream", false));

        SharingUtils.setSharing(requestSpec, new SharingRequest(
                new SharingRequest.Entity(SharingUtils.ENTITY_STREAM, stream2Id),
                ImmutableMap.of(
                        new SharingRequest.Entity(SharingUtils.ENTITY_USER, userId), SharingUtils.PERMISSION_VIEW
                )));

        GelfInputUtils.createGelfHttpInput(sut, 12201, requestSpec)
                .postMessage("""
                        {"short_message":"search-sync-test", "host":"example.org", "facility":"test", "_level":1, "_target_stream": "stream1"}
                        """)
                .postMessage("""
                        {"short_message":"search-sync-test-2", "host":"example.org", "facility":"another-test", "_level":2, "_target_stream": "stream2"}
                        """)
                .postMessage("""
                        {"short_message":"search-sync-test-3", "host":"lorem-ipsum.com", "facility":"another-test", "_level":3, "_http_method":"POST", "_target_stream": "stream2"}
                        """)
                .waitForAllMessages();
    }

    @ContainerMatrixTest
    void testUserWithLimitedPermissionRequest() {

        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .auth().basic("john.doe", "asdfgh")
                .when()
                .body("""
                         {
                           "group_by": [
                             {
                               "field": "facility"
                             }
                           ],
                           "metrics": [
                             {
                               "function": "count",
                               "field": "facility"
                             }
                           ]
                        }
                         """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validatableResponse.assertThat().body("data.rows", Matchers.hasSize(1));
        validateRow(validatableResponse, "another-test", 2);
    }

    @ContainerMatrixTest
    void testSchema() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            }
                          ]
                          }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateSchema(validatableResponse, "Grouping", "string", "facility");
        validateSchema(validatableResponse, "Metric : count", "numeric", "facility");
    }

    @ContainerMatrixTest
    void testMinimalRequest() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2);
        validateRow(validatableResponse, "test", 1);
    }

    @ContainerMatrixTest
    void testAsciiRender() {
        final String response = given()
                .spec(requestSpec)
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().trim();

        String expected = """
                ┌────────────────────────┬───────────────────────┐
                │Grouping                │Metric : count         │
                │facility                │facility               │
                │string                  │numeric                │
                ├────────────────────────┼───────────────────────┤
                │another-test            │2                      │
                │test                    │1                      │
                └────────────────────────┴───────────────────────┘
                """;

        org.assertj.core.api.Assertions.assertThat(response).isEqualTo(expected.trim());
    }

    @ContainerMatrixTest
    void testGetRequestAcii() {
        final String response = given()
                .spec(requestSpec)
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .when()
                .get("/search/aggregate?groups=facility&metrics=count:facility")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().trim();

        String expected = """
                ┌────────────────────────┬───────────────────────┐
                │Grouping                │Metric : count         │
                │facility                │facility               │
                │string                  │numeric                │
                ├────────────────────────┼───────────────────────┤
                │another-test            │2                      │
                │test                    │1                      │
                └────────────────────────┴───────────────────────┘
                """;
        org.assertj.core.api.Assertions.assertThat(response).isEqualTo(expected.trim());
    }

    @ContainerMatrixTest
    void testGetRequestJson() {
        final ValidatableResponse response = given()
                .spec(requestSpec)
                .header(new Header("Accept", MediaType.APPLICATION_JSON))
                .when()
                .get("/search/aggregate?groups=facility&metrics=count:facility")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(response, "another-test", 2);
        validateRow(response, "test", 1);
    }

    @ContainerMatrixTest
    void testTwoAggregations() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            },
                            {
                              "function": "latest",
                              "field": "level"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2, 3);
        validateRow(validatableResponse, "test", 1, 1);
    }

    @ContainerMatrixTest
    void testDuplicatedMetrics() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            },
                            {
                              "function": "count",
                              "field": "facility"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .statusCode(404); // TODO! We should handle the duplicated metric better
    }

    @ContainerMatrixTest
    void testAggregationWithoutMatchingField() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "http_method"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "POST", 1);
        validateRow(validatableResponse, "(Empty Value)", 2);
    }

    @ContainerMatrixTest
    void testMissingDataInRow() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility"
                            },
                            {
                              "function": "latest",
                              "field": "http_method"
                            },
                             {
                              "function": "latest",
                              "field": "level"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2, "POST", 3);
        validateRow(validatableResponse, "test", 1, "-", 1);
    }

    @ContainerMatrixTest
    void testStreamFiltering() {
        final String req = """
                {
                   "streams": ["%s"],
                  "group_by": [
                    {
                      "field": "facility"
                    }
                  ],
                  "metrics": [
                    {
                      "function": "count",
                      "field": "facility"
                    }
                  ]
                }
                """;
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body(String.format(Locale.ROOT, req, stream2Id))
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2);
        validatableResponse.assertThat().body("data.rows", Matchers.hasSize(1));
    }

    @ContainerMatrixTest
    void testSorting() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility",
                              "sort": "asc"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        final List<List<Object>> rows = validatableResponse.extract().body().jsonPath().getList("data.rows");
        Assertions.assertEquals(rows.size(), 2);
        Assertions.assertEquals(Arrays.asList("test", (Object) 1), rows.get(0));
        Assertions.assertEquals(Arrays.asList("another-test", (Object) 2), rows.get(1));
    }

    @ContainerMatrixTest
    void testMetadata() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                            "timerange": {
                                "type": "relative",
                                "range": 300
                            },
                          "group_by": [
                            {
                              "field": "facility"
                            }
                          ],
                          "metrics": [
                            {
                              "function": "count",
                              "field": "facility",
                              "sort": "asc"
                            }
                          ]
                          }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validatableResponse.assertThat().body("metadata.effective_timerange.type", Matchers.equalTo("absolute"));
        final String from = validatableResponse.extract().body().jsonPath().getString("metadata.effective_timerange.from");
        final String to = validatableResponse.extract().body().jsonPath().getString("metadata.effective_timerange.to");
        final DateTime fromDateTime = DateTime.parse(from);
        final DateTime toDateTime = DateTime.parse(to);
        final long diff = toDateTime.getMillis() - fromDateTime.getMillis();
        Assertions.assertEquals(300_000, diff);
    }


    private void validateSchema(ValidatableResponse response, String name, String type, String field) {
        response.assertThat().body("schema", Matchers.hasItem(
                Matchers.allOf(
                        entry("name", name),
                        entry("type", type),
                        entry("field", field)
                )
        ));
    }

    /**
     * Each data row consist of an array, containing key as the first item, followed by values for each metric.
     */
    private void validateRow(ValidatableResponse response, String key, Object... values) {
        final ArrayList<Object> expected = new ArrayList<>();
        expected.add(key);
        expected.addAll(Arrays.asList(values));

        response.assertThat().body("data.rows", Matchers.hasItem(Matchers.equalTo(expected)));
    }

    private Matcher<Map<? extends String, ?>> entry(String key, Object value) {
        return hasEntry(key, value);
    }
}
