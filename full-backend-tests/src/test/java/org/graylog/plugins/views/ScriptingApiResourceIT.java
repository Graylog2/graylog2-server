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
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Sharing;
import org.graylog.testing.completebackend.apis.SharingRequest;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.MoreMediaTypes;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.Csv;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvParser;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.within;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;

@ContainerMatrixTestsConfiguration(serverLifecycle = CLASS, mongoVersions = MongodbServer.MONGO5,
                                   searchVersions = {SearchServer.ES7, SearchServer.OS2},
                                   enabledFeatureFlags = ScriptingApiModule.FEATURE_FLAG)
public class ScriptingApiResourceIT {

    private final RequestSpecification requestSpec;
    private final GraylogApis api;

    private String stream1Id;
    private String stream2Id;

    public ScriptingApiResourceIT(RequestSpecification requestSpec, GraylogApis apis) {
        this.requestSpec = requestSpec;
        this.api = apis;
    }

    @BeforeAll
    public void beforeAll() {

        final String defaultIndexSetId = api.indices().defaultIndexSetId();

        final JsonPath user = api.users().createUser(new Users.User(
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


        this.stream1Id = api.streams().createStream("Stream #1", defaultIndexSetId, new Streams.StreamRule(StreamRuleType.EXACT.toInteger(), "stream1", "target_stream", false));
        this.stream2Id = api.streams().createStream("Stream #2", defaultIndexSetId, new Streams.StreamRule(StreamRuleType.EXACT.toInteger(), "stream2", "target_stream", false));

        api.sharing().setSharing(new SharingRequest(
                new SharingRequest.Entity(Sharing.ENTITY_STREAM, stream2Id),
                ImmutableMap.of(
                        new SharingRequest.Entity(Sharing.ENTITY_USER, userId), Sharing.PERMISSION_VIEW
                )));

        api.gelf()
                .createGelfHttpInput(12201)
                .postMessage("""
                        {"short_message":"search-sync-test", "host":"example.org", "facility":"test", "_level":1, "_target_stream": "stream1"}
                        """)
                .postMessage("""
                        {"short_message":"search-sync-test-2", "host":"example.org", "facility":"another-test", "_level":2, "_target_stream": "stream2"}
                        """)
                .postMessage("""
                        {"short_message":"search-sync-test-3", "host":"lorem-ipsum.com", "facility":"another-test", "_level":3, "_http_method":"POST", "_target_stream": "stream2"}
                        """);

        api.search().waitForMessagesCount(3);
        api.fieldTypes().waitForFieldTypeDefinitions( "source", "facility", "level");
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

        validatableResponse.assertThat().body("datarows", Matchers.hasSize(1));
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

        validateSchema(validatableResponse, "grouping: facility", "string", "facility");
        validateSchema(validatableResponse, "metric: count(facility)", "numeric", "facility");
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
                │grouping: facility      │metric: count(facility)│
                ├────────────────────────┼───────────────────────┤
                │another-test            │2                      │
                │test                    │1                      │
                └────────────────────────┴───────────────────────┘
                """;

        assertThat(response).isEqualTo(expected.trim());
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
                │grouping: facility      │metric: count(facility)│
                ├────────────────────────┼───────────────────────┤
                │another-test            │2                      │
                │test                    │1                      │
                └────────────────────────┴───────────────────────┘
                """;
        assertThat(response).isEqualTo(expected.trim());
    }

    @ContainerMatrixTest
    void testCsvRender() {
        final InputStream response = given()
                .spec(requestSpec)
                .header(new Header("Accept", MoreMediaTypes.TEXT_CSV))
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
                .extract().body().asInputStream();

        final CsvParser csvParser = new CsvParser(Csv.parseRfc4180());
        final List<String[]> lines = csvParser.parseAll(response);



        // headers
        Assertions.assertArrayEquals(lines.get(0), new String[]{"grouping: facility", "metric: count(facility)"});

        //rows
        Assertions.assertArrayEquals(lines.get(1), new String[]{"another-test", "2"});
        Assertions.assertArrayEquals(lines.get(2), new String[]{"test", "1"});
    }

    @ContainerMatrixTest
    void testGetRequestCsv() {

        final InputStream response = given()
                .spec(requestSpec)
                .header(new Header("Accept", MoreMediaTypes.TEXT_CSV))
                .when()
                .get("/search/aggregate?groups=facility&metrics=count:facility")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asInputStream();


        final CsvParser csvParser = new CsvParser(Csv.parseRfc4180());
        final List<String[]> lines = csvParser.parseAll(response);

        // headers
        Assertions.assertArrayEquals(lines.get(0), new String[]{"grouping: facility", "metric: count(facility)"});

        //rows
        Assertions.assertArrayEquals(lines.get(1), new String[]{"another-test", "2"});
        Assertions.assertArrayEquals(lines.get(2), new String[]{"test", "1"});
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
                              "function": "max",
                              "field": "level"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2, 3.0f);
        validateRow(validatableResponse, "test", 1, 1.0f);
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
                .statusCode(200);
        validateRow(validatableResponse, "another-test", 2, 2);
        validateRow(validatableResponse, "test", 1, 1);
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
                              "function": "max",
                              "field": "level"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2, "POST", 3.0f);
        validateRow(validatableResponse, "test", 1, "-", 1.0f);
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
        validatableResponse.assertThat().body("datarows", Matchers.hasSize(1));
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

        final List<List<Object>> rows = validatableResponse.extract().body().jsonPath().getList("datarows");
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
        final float diff = toDateTime.getMillis() - fromDateTime.getMillis();
        assertThat(diff).isCloseTo(300_000, within(10_000f));
    }

    @ContainerMatrixTest
    void testErrorHandling() {
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
                              "function": "max",
                              "field": "facility"
                            }
                          ]
                        }
                        """)
                .post("/search/aggregate")
                .then()
                .statusCode(400)
                .assertThat()
                .body("type", Matchers.equalTo("ApiError"))
                .body("message", Matchers.containsString("Failed to obtain results"));
    }

    @ContainerMatrixTest
    void testMessages() {
        final ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "fields": ["source", "facility", "level"]
                        }
                        """)
                .post("/search/messages")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateSchema(validatableResponse, "field: source", "string", "source");
        validateSchema(validatableResponse, "field: facility", "string", "facility");
        validateSchema(validatableResponse, "field: level", "numeric", "level");

        validateRow(validatableResponse, "lorem-ipsum.com", "another-test", 3);
        validateRow(validatableResponse, "example.org", "another-test", 2);
        validateRow(validatableResponse, "example.org", "test", 1);

    }

    @ContainerMatrixTest
    void testMessagesWithSorting() {
        ValidatableResponse validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "fields": ["source", "facility", "level"],
                          "sort": "level",
                          "sort_order" : "Descending"
                        }
                        """)
                .post("/search/messages")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        List<List<Object>> rows = validatableResponse.extract().body().jsonPath().getList("datarows");
        Assertions.assertEquals(rows.size(), 3);
        assertThat(rows.get(0)).contains(3);
        assertThat(rows.get(1)).contains(2);
        assertThat(rows.get(2)).contains(1);

        validatableResponse = given()
                .spec(requestSpec)
                .when()
                .body("""
                        {
                          "fields": ["source", "facility", "level"],
                          "sort": "facility",
                          "sort_order" : "Ascending"
                        }
                        """)
                .post("/search/messages")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        rows = validatableResponse.extract().body().jsonPath().getList("datarows");
        Assertions.assertEquals(rows.size(), 3);
        assertThat(rows.get(0)).contains("another-test");
        assertThat(rows.get(1)).contains("another-test");
        assertThat(rows.get(2)).contains("test");

    }

    @ContainerMatrixTest
    void testMessagesGetRequestAscii() {
        final List<String> response = given()
                .spec(requestSpec)
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .get("/search/messages?fields=source,facility,level")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .extract().body().asString().strip().lines().toList();

        final List<String> expected = """
                ┌────────────────────────┬────────────────────────┬───────────────────────┐
                │field: source           │field: facility         │field: level           │
                ├────────────────────────┼────────────────────────┼───────────────────────┤
                │lorem-ipsum.com         │another-test            │3                      │
                │example.org             │another-test            │2                      │
                │example.org             │test                    │1                      │
                └────────────────────────┴────────────────────────┴───────────────────────┘
                """.strip().lines().toList();

        assertThat(response.size()).isEqualTo(expected.size());
        assertThat(expected.containsAll(response)).isTrue();
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

        response.assertThat().body("datarows", Matchers.hasItem(Matchers.equalTo(expected)));
    }

    private Matcher<Map<? extends String, ?>> entry(String key, Object value) {
        return hasEntry(key, value);
    }
}
