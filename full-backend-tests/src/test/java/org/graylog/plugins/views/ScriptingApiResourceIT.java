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

import au.com.bytecode.opencsv.CSVParser;
import io.restassured.http.Header;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.MediaType;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Sharing;
import org.graylog.testing.completebackend.apis.SharingRequest;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.rest.MoreMediaTypes;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasEntry;

@GraylogBackendConfiguration(serverLifecycle = CLASS)
public class ScriptingApiResourceIT {

    public static final String DEFAULT_STREAM = "000000000000000000000001";
    private static GraylogApis api;

    private static String stream1Id;
    private static String stream2Id;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        api = graylogApis;

        final String defaultIndexSetId = api.indices().defaultIndexSetId();

        final JsonPath user = api.users().createUser(Users.JOHN_DOE);

        final String userId = user.getString("id");


        stream1Id = api.streams().createStream("Stream #1", defaultIndexSetId, Streams.StreamRule.exact("stream1", "target_stream", false));
        stream2Id = api.streams().createStream("Stream #2", defaultIndexSetId, Streams.StreamRule.exact("stream2", "target_stream", false));

        api.sharing().setSharing(new SharingRequest(
                new SharingRequest.Entity(Sharing.ENTITY_STREAM, stream2Id),
                Map.of(
                        new SharingRequest.Entity(Sharing.ENTITY_USER, userId), Sharing.PERMISSION_VIEW
                )));

        api.gelf()
                .createGelfHttpInput(12201)
                .postMessage("""
                        {"short_message":"search-sync-test", "host":"example.org", "facility":"test", "_level":1, "_target_stream": "stream1", "date_field": "2025-10-20T12:00:10.123Z"}
                        """)
                .postMessage("""
                        {"short_message":"search-sync-test-2", "host":"example.org", "facility":"another-test", "_level":2, "_target_stream": "stream2", "date_field": "2025-10-20T12:10:42.123Z"}
                        """)
                .postMessage("""
                        {"short_message":"search-sync-test-3", "host":"lorem-ipsum.com", "facility":"another-test", "_level":3, "_http_method":"POST", "_target_stream": "stream2", "date_field": "2025-10-21T12:01:30.123Z"}
                        """);

        api.search().waitForMessages("search-sync-test", "search-sync-test-2", "search-sync-test-3");
        api.fieldTypes().waitForFieldTypeDefinitions("source", "facility", "level", "date_field");
    }

    @FullBackendTest
    void testAggregationByStream() {
        final ValidatableResponse validatableResponse =
                api.post("/search/aggregate", """
                         {
                           "group_by": [
                             {
                               "field": "streams.id"
                             }
                           ],
                           "metrics": [
                             {
                               "function": "count"
                             }
                           ]
                        }
                        """, 200);

        validatableResponse.log().ifValidationFails()
                .assertThat().body("datarows", Matchers.hasSize(3));

        validateRow(validatableResponse, DEFAULT_STREAM, 3);
        validateRow(validatableResponse, stream2Id, 2);
        validateRow(validatableResponse, stream1Id, 1);
    }

    @FullBackendTest
    void testStdDevSorting() {
        final GraylogApiResponse responseDesc =
                new GraylogApiResponse(api.post("/search/aggregate", """
                        {
                        	"group_by": [
                        		{
                        			"field": "facility"
                        		}
                        	],
                        	"metrics": [
                        		{
                        			"function": "stddev",
                        			"field": "level",
                        			"sort": "desc"
                        		}
                        	]
                        }
                        """, 200));

        responseDesc.validatableResponse().log().ifValidationFails()
                .assertThat().body("datarows", Matchers.hasSize(2));

        List<Double> stddevDesc = responseDesc.properJSONPath().read("datarows.*[1]");
        org.assertj.core.api.Assertions.assertThat(stddevDesc)
                .hasSize(2)
                .containsExactly(0.5, 0.0);

        final GraylogApiResponse responseAsc =
                new GraylogApiResponse(api.post("/search/aggregate", """
                        {
                        	"group_by": [
                        		{
                        			"field": "facility"
                        		}
                        	],
                        	"metrics": [
                        		{
                        			"function": "stddev",
                        			"field": "level",
                        			"sort": "asc"
                        		}
                        	]
                        }
                        """, 200));

        List<Double> stddevAsc = responseAsc.properJSONPath().read("datarows.*[1]");
        org.assertj.core.api.Assertions.assertThat(stddevAsc)
                .hasSize(2)
                .containsExactly(0.0, 0.5);
    }

    @FullBackendTest
    void testAggregationByStreamTitle() {
        final ValidatableResponse validatableResponse =
                api.post("/search/aggregate", """
                         {
                           "group_by": [
                             {
                               "field": "streams.title"
                             }
                           ],
                           "metrics": [
                             {
                               "function": "count"
                             }
                           ]
                        }
                        """, 200);

        validatableResponse.log().ifValidationFails()
                .assertThat().body("datarows", Matchers.hasSize(3));

        validateRow(validatableResponse, "Default Stream", 3);
        validateRow(validatableResponse, "Stream #2", 2);
        validateRow(validatableResponse, "Stream #1", 1);
    }

    @FullBackendTest
    void testUserWithLimitedPermissionRequest() {

        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testSchema() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testMinimalRequest() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testAsciiRender() {
        final String response = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testGetRequestAcii() {
        final String response = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testCsvRender() throws Exception {
        final InputStream response = given()
                .spec(api.requestSpecification())
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

        final List<String[]> lines = parseCsvLines(response);

        // headers
        Assertions.assertArrayEquals(new String[]{"grouping: facility", "metric: count(facility)"}, lines.get(0));

        //rows
        Assertions.assertArrayEquals(new String[]{"another-test", "2"}, lines.get(1));
        Assertions.assertArrayEquals(new String[]{"test", "1"}, lines.get(2));
    }

    private List<String[]> parseCsvLines(InputStream inputStream) throws Exception {
        final CSVParser csvParser = new CSVParser(',', '"');
        final List<String[]> lines = new ArrayList<>();

        try (final var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            while (reader.ready()) {
                lines.add(csvParser.parseLine(reader.readLine()));
            }
        }

        return lines;
    }

    @FullBackendTest
    void testGetRequestCsv() throws Exception {

        final InputStream response = given()
                .spec(api.requestSpecification())
                .header(new Header("Accept", MoreMediaTypes.TEXT_CSV))
                .when()
                .get("/search/aggregate?groups=facility&metrics=count:facility")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asInputStream();


        final List<String[]> lines = parseCsvLines(response);

        // headers
        Assertions.assertArrayEquals(new String[]{"grouping: facility", "metric: count(facility)"}, lines.get(0));

        //rows
        Assertions.assertArrayEquals(new String[]{"another-test", "2"}, lines.get(1));
        Assertions.assertArrayEquals(new String[]{"test", "1"}, lines.get(2));
    }

    @FullBackendTest
    void testGetRequestJson() {
        final ValidatableResponse response = given()
                .spec(api.requestSpecification())
                .header(new Header("Accept", MediaType.APPLICATION_JSON))
                .when()
                .get("/search/aggregate?groups=facility&metrics=count:facility")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(response, "another-test", 2);
        validateRow(response, "test", 1);
    }

    @FullBackendTest
    void testTwoAggregations() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testDuplicatedMetrics() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testAggregationWithoutMatchingField() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testMissingDataInRow() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
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
                .spec(api.requestSpecification())
                .when()
                .body(String.format(Locale.ROOT, req, stream2Id))
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        validateRow(validatableResponse, "another-test", 2);
        validatableResponse.assertThat().body("datarows", Matchers.hasSize(1));
    }

    @FullBackendTest
    void testSorting() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals(Arrays.asList("test", (Object) 1), rows.get(0));
        Assertions.assertEquals(Arrays.asList("another-test", (Object) 2), rows.get(1));
    }

    @FullBackendTest
    void testMetadata() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testErrorHandling() {
        final ValidatableResponse ignored = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testMessages() {
        final ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testMessagesWithSorting() {
        ValidatableResponse validatableResponse = given()
                .spec(api.requestSpecification())
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
        Assertions.assertEquals(3, rows.size());
        assertThat(rows.get(0)).contains(3);
        assertThat(rows.get(1)).contains(2);
        assertThat(rows.get(2)).contains(1);

        validatableResponse = given()
                .spec(api.requestSpecification())
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
        Assertions.assertEquals(3, rows.size());
        assertThat(rows.get(0)).contains("another-test");
        assertThat(rows.get(1)).contains("another-test");
        assertThat(rows.get(2)).contains("test");

    }

    @FullBackendTest
    void testMessagesGetRequestAscii() {
        final List<String> response = given()
                .spec(api.requestSpecification())
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

    @FullBackendTest
    void testPercentageMetric() {
        final String req = """
                {
                  "group_by": [
                    {
                      "field": "facility"
                    }
                  ],
                  "metrics": [
                    {
                      "function": "percentage"
                    }
                  ]
                }
                """;
        final var response = given()
                .spec(api.requestSpecification())
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .body(req)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().strip().lines().toList();

        final List<String> percentageMetricExpectedResult = """
                ┌────────────────────────┬───────────────────────┐
                │grouping: facility      │metric: percentage()   │
                ├────────────────────────┼───────────────────────┤
                │another-test            │0.6666666666666666     │
                │test                    │0.3333333333333333     │
                └────────────────────────┴───────────────────────┘
                """.strip().lines().toList();

        assertThat(response.size()).isEqualTo(percentageMetricExpectedResult.size());
        assertThat(percentageMetricExpectedResult.containsAll(response)).isTrue();
    }

    @FullBackendTest
    void testPercentageMetricWithFieldName() {
        final String req = """
                {
                  "group_by": [
                    {
                      "field": "facility"
                    }
                  ],
                  "metrics": [
                    {
                      "function": "percentage",
                      "field": "facility"
                    }
                  ]
                }
                """;
        final var response = given()
                .spec(api.requestSpecification())
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .body(req)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().strip().lines().toList();

        final List<String> percentageMetricExpectedResult = """
                ┌────────────────────────┬───────────────────────┐
                │grouping: facility      │metric:                │
                │                        │percentage(facility)   │
                ├────────────────────────┼───────────────────────┤
                │another-test            │0.6666666666666666     │
                │test                    │0.3333333333333333     │
                └────────────────────────┴───────────────────────┘
                """.strip().lines().toList();

        assertThat(response.size()).isEqualTo(percentageMetricExpectedResult.size());
        assertThat(percentageMetricExpectedResult.containsAll(response)).isTrue();
    }

    @FullBackendTest
    void testPercentageMetricWithConfig() {
        final String req = """
                {
                  "group_by": [
                    {
                      "field": "facility"
                    }
                  ],
                  "metrics": [
                    {
                      "function": "percentage",
                      "field": "facility",
                      "configuration" : {
                        "strategy" : "COUNT"
                      }
                    }
                  ]
                }
                """;
        final var response = given()
                .spec(api.requestSpecification())
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .body(req)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().strip().lines().toList();

        final List<String> percentageMetricExpectedResult = """
                ┌────────────────────────┬───────────────────────┐
                │grouping: facility      │metric:                │
                │                        │percentage(facility)   │
                ├────────────────────────┼───────────────────────┤
                │another-test            │0.6666666666666666     │
                │test                    │0.3333333333333333     │
                └────────────────────────┴───────────────────────┘
                """.strip().lines().toList();

        assertThat(response.size()).isEqualTo(percentageMetricExpectedResult.size());
        assertThat(percentageMetricExpectedResult.containsAll(response)).isTrue();
    }

    @FullBackendTest
    void testTimestampGroupingBy1Hour() {
        api.waitFor(() -> {
            var fieldTypes = api.fieldTypes().getFieldTypes(RelativeRange.allTime(), Set.of(stream1Id, stream2Id));
            return fieldTypes.stream().anyMatch(type -> type.name().equals("date_field") && type.type().equals(FieldTypeMapper.DATE_TYPE));
        }, "'date_field' should exist as a date type field in graylog so that bucketing works");

        final var req = """
                        {
                          "timerange": {
                            "type": "relative",
                            "range": 0
                           },
                           "group_by": [
                             {
                               "field": "date_field",
                               "timeunit": "1h"
                             }
                           ],
                           "metrics": [
                             {
                               "function": "count"
                             }
                           ]
                        }
                        """;

        final var response = given()
                .spec(api.requestSpecification())
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .body(req)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().strip().lines().toList();

        final List<String> expectedResult = """
                                            ┌────────────────────────┬───────────────────────┐
                                            │grouping: date_field    │metric: count()        │
                                            ├────────────────────────┼───────────────────────┤
                                            │2025-10-20T12:00:00.000Z│2                      │
                                            │2025-10-20T13:00:00.000Z│0                      │
                                            │2025-10-20T14:00:00.000Z│0                      │
                                            │2025-10-20T15:00:00.000Z│0                      │
                                            │2025-10-20T16:00:00.000Z│0                      │
                                            │2025-10-20T17:00:00.000Z│0                      │
                                            │2025-10-20T18:00:00.000Z│0                      │
                                            │2025-10-20T19:00:00.000Z│0                      │
                                            │2025-10-20T20:00:00.000Z│0                      │
                                            │2025-10-20T21:00:00.000Z│0                      │
                                            │2025-10-20T22:00:00.000Z│0                      │
                                            │2025-10-20T23:00:00.000Z│0                      │
                                            │2025-10-21T00:00:00.000Z│0                      │
                                            │2025-10-21T01:00:00.000Z│0                      │
                                            │2025-10-21T02:00:00.000Z│0                      │
                                            │2025-10-21T03:00:00.000Z│0                      │
                                            │2025-10-21T04:00:00.000Z│0                      │
                                            │2025-10-21T05:00:00.000Z│0                      │
                                            │2025-10-21T06:00:00.000Z│0                      │
                                            │2025-10-21T07:00:00.000Z│0                      │
                                            │2025-10-21T08:00:00.000Z│0                      │
                                            │2025-10-21T09:00:00.000Z│0                      │
                                            │2025-10-21T10:00:00.000Z│0                      │
                                            │2025-10-21T11:00:00.000Z│0                      │
                                            │2025-10-21T12:00:00.000Z│1                      │
                                            └────────────────────────┴───────────────────────┘
                                            """.strip().lines().toList();

        assertThat(response.size())
                .withFailMessage("Comparison failed:\n\n%s\n\nbut expected:\n\n%s", String.join("\n", response), String.join("\n", expectedResult))
                .isEqualTo(expectedResult.size());
        assertThat(expectedResult.containsAll(response))
                .withFailMessage("Comparison failed:\n\n%s\n\nbut expected:\n\n%s", String.join("\n", response), String.join("\n", expectedResult))
                .isTrue();
    }

    @FullBackendTest
    void testTimestampGroupingBy1Day() {
        api.waitFor(() -> {
            var fieldTypes = api.fieldTypes().getFieldTypes(RelativeRange.allTime(), Set.of(stream1Id, stream2Id));
            return fieldTypes.stream().anyMatch(type -> type.name().equals("date_field") && type.type().equals(FieldTypeMapper.DATE_TYPE));
        }, "'date_field' should exist as a date type field in graylog so that bucketing works");

        final var req = """
                        {
                          "timerange": {
                            "type": "relative",
                            "range": 0
                           },
                           "group_by": [
                             {
                               "field": "date_field",
                               "timeunit": "1d"
                             }
                           ],
                           "metrics": [
                             {
                               "function": "count"
                             }
                           ]
                        }
                        """;

        final var response = given()
                .spec(api.requestSpecification())
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .body(req)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().strip().lines().toList();

        final List<String> expectedResult = """
                                            ┌────────────────────────┬───────────────────────┐
                                            │grouping: date_field    │metric: count()        │
                                            ├────────────────────────┼───────────────────────┤
                                            │2025-10-20T00:00:00.000Z│2                      │
                                            │2025-10-21T00:00:00.000Z│1                      │
                                            └────────────────────────┴───────────────────────┘
                                            """.strip().lines().toList();

        assertThat(response.size())
                .withFailMessage("Comparison failed:\n\n%s\n\nbut expected:\n\n%s", String.join("\n", response), String.join("\n", expectedResult))
                .isEqualTo(expectedResult.size());
        assertThat(expectedResult.containsAll(response))
                .withFailMessage("Comparison failed:\n\n%s\n\nbut expected:\n\n%s", String.join("\n", response), String.join("\n", expectedResult))
                .isTrue();
    }

    @FullBackendTest
    void testTimestampGroupingByScaling() {
        api.waitFor(() -> {
            var fieldTypes = api.fieldTypes().getFieldTypes(RelativeRange.allTime(), Set.of(stream1Id, stream2Id));
            return fieldTypes.stream().anyMatch(type -> type.name().equals("date_field") && type.type().equals(FieldTypeMapper.DATE_TYPE));
        }, "'date_field' should exist as a date type field in graylog so that bucketing works");

        final var req = """
                        {
                          "timerange": {
                            "type": "relative",
                            "range": 0
                           },
                           "group_by": [
                             {
                               "field": "date_field",
                               "scaling": 3.0
                             }
                           ],
                           "metrics": [
                             {
                               "function": "count"
                             }
                           ]
                        }
                        """;

        final var response = given()
                .spec(api.requestSpecification())
                .when()
                .header(new Header("Accept", MediaType.TEXT_PLAIN))
                .body(req)
                .post("/search/aggregate")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200)
                .extract().body().asString().strip().lines().toList();

        final List<String> expectedResult = """
                                            ┌────────────────────────┬───────────────────────┐
                                            │grouping: date_field    │metric: count()        │
                                            ├────────────────────────┼───────────────────────┤
                                            │2025-10-20T12:00:00.000Z│2                      │
                                            │2025-10-21T00:00:00.000Z│0                      │
                                            │2025-10-21T12:00:00.000Z│1                      │
                                            └────────────────────────┴───────────────────────┘
                                            """.strip().lines().toList();

        assertThat(response.size())
                .withFailMessage("Comparison failed:\n\n%s\n\nbut expected:\n\n%s", String.join("\n", response), String.join("\n", expectedResult))
                .isEqualTo(expectedResult.size());
        assertThat(expectedResult.containsAll(response))
                .withFailMessage("Comparison failed:\n\n%s\n\nbut expected:\n\n%s", String.join("\n", response), String.join("\n", expectedResult))
                .isTrue();
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
