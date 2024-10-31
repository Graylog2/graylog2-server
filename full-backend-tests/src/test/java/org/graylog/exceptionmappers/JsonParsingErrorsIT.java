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
package org.graylog.exceptionmappers;

import io.restassured.response.ValidatableResponse;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.OS2_LATEST)
public class JsonParsingErrorsIT {
    private static final String SYNC_SEARCH = "/views/search/sync";
    private static final String STREAMS = "/streams";

    private final GraylogApis api;

    public JsonParsingErrorsIT(GraylogApis api) {
        this.api = api;
    }

    @ContainerMatrixTest
    void returnsSpecificErrorWhenTypeMismatches() {
        assertErrorResponse(SYNC_SEARCH, """
                        {
                         	"queries": [
                         		{
                         			"id": "75988e96-71e2-4f3f-9d14-d8e918571b16",
                         			"query": {
                         				"type": "elasticsearch",
                         				"query_string": ""
                         			},
                         			"timerange": {
                         				"type": "relative",
                         				"from": "foo"
                         			}
                         		}
                         	]
                         }
                                                """)
                .body("path", equalTo("queries.[0].timerange.from"))
                .body("line", equalTo(11))
                .body("column", equalTo(14))
                .body("message", equalTo("Error at \"queries.[0].timerange.from\" [11, 14]: Must be of type int"))
                .body("reference_path", equalTo(
                        "org.graylog.plugins.views.search.rest.AutoValue_SearchDTO$Builder[\"queries\"]" +
                                "->java.util.LinkedHashSet[0]->org.graylog.plugins.views.search.rest.AutoValue_QueryDTO$Builder[\"timerange\"]" +
                                "->org.graylog2.plugin.indexer.searches.timeranges.AutoValue_RelativeRange$Builder[\"from\"]"));

    }

    @ContainerMatrixTest
    void returnsSpecificErrorForJsonParsingError() {
        assertErrorResponse(SYNC_SEARCH, """
                        {
                         	"queries": [
                         		{
                         			"id": "75988e96-71e2-4f3f-9d14-d8e918571b16",
                         			"query": {
                         				"type": "elasticsearch",
                         				"query_string": ""
                         			},
                         			"timerange": {
                         				"type": "relative",
                         				"from": 23,
                         			}
                         		}
                         	]
                         }
                                                """)
                .body("path", equalTo("queries.[0].timerange"))
                .body("line", equalTo(12))
                .body("column", equalTo(5))
                .body("message", containsString("Unexpected character ('}' (code 125)): was expecting double-quote to start field name"))
                .body("reference_path", equalTo(
                        "org.graylog.plugins.views.search.rest.AutoValue_SearchDTO$Builder[\"queries\"]" +
                                "->java.util.LinkedHashSet[0]" +
                                "->org.graylog.plugins.views.search.rest.AutoValue_QueryDTO$Builder[\"timerange\"]"));

    }

    @ContainerMatrixTest
    void extractsReferencePathFromMissingProperty() {
        assertErrorResponse(STREAMS, "{}")
                .body("reference_path", equalTo("org.graylog2.rest.resources.streams.requests.CreateStreamRequest"));

        assertErrorResponse(STREAMS, """
                {
                    "title": "Foo",
                    "rules": [{}]
                }
                """)
                .body("reference_path", equalTo(
                        "org.graylog2.rest.resources.streams.requests.CreateStreamRequest[\"rules\"]" +
                                "->java.util.ArrayList[0]" +
                                "->org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest"));
    }

    @ContainerMatrixTest
    void handlesGenericJSONErrorsOnRootLevel() {
        assertErrorResponse(STREAMS, """
                {
                    "title": "Foo",
                }
                """)
                .body("message", equalTo("Unexpected character ('}' (code 125)): was expecting double-quote to start field name"))
                .body("line", equalTo(3))
                .body("column", equalTo(1));

        assertErrorResponse(STREAMS, """
                {
                    "title":: "Foo"
                }
                """)
                .body("message", equalTo("Unexpected character (':' (code 58)): expected a valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')"))
                .body("line", equalTo(2))
                .body("column", equalTo(13));
    }

    @ContainerMatrixTest
    void handleInvalidPropertiesOnRootLevel() {
        assertErrorResponse(SYNC_SEARCH, """
                {
                    "foo": 23
                }
                """)
                .body("message", equalTo("Unable to map property foo.\nKnown properties include: parameters, id, queries, skip_no_streams_check"))
                .body("line", equalTo(2))
                .body("column", equalTo(14))
                .body("path", equalTo("foo"))
                .body("reference_path", equalTo("org.graylog.plugins.views.search.rest.AutoValue_SearchDTO$Builder[\"foo\"]"));
    }

    private ValidatableResponse assertErrorResponse(String url, String body) {
        return given()
                .spec(api.requestSpecification())
                .log().ifValidationFails()
                .when()
                .body(body)
                .post(url)
                .then()
                .log().ifValidationFails()
                .assertThat()
                .statusCode(400)
                .body("type", equalTo("RequestError"));
    }
}
