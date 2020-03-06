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
package org.graylog.testing.fullbackend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;


@ApiIntegrationTest(serverLifecycle = CLASS)
class BackendStartupIT {

    private final GraylogBackend sut;

    public BackendStartupIT(GraylogBackend sut) {
        this.sut = sut;
    }

    @BeforeAll
    static void beforeAll() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        RestAssured.requestSpecification =
                new RequestSpecBuilder().build()
                        .accept(JSON)
                        .contentType(JSON)
                        .header("X-Requested-By", "peterchen")
                        .auth().basic("admin", "admin");
    }

    @Test
    void canReachApi() {
        when()
                .get(sut.apiAddress())
                .then()
                .statusCode(200);
    }

    @Test
    void loadsDefaultPlugins() {
        List<Object> pluginNames = when()
                .get(sut.apiAddress() + "/system/plugins")
                .then()
                .statusCode(200)
                .extract().jsonPath()
                .getList("plugins.name");

        assertThat(pluginNames).containsExactlyInAnyOrder(
                "Threat Intelligence Plugin",
                "Collector",
                "AWS plugins");
    }

    @Test
    void importsElasticsearchFixtures() throws InvalidRangeParametersException, JsonProcessingException {
        sut.importElasticsearchFixture("one-message.json", getClass());

        MessageList messageList = MessageList.builder().id("123").build();
        Query q = Query.builder()
                .id("123")
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .timerange(AbsoluteRange.create("2010-01-01T00:00:00.0Z", "2050-01-01T00:00:00.0Z"))
                .searchTypes(ImmutableSet.of(messageList))
                .build();
        Search s = Search.builder().queries(ImmutableSet.of(q)).build();

        String body = new ObjectMapperProvider().get().writeValueAsString(s);

        List<String> j = given().when()
                .body(body)
                .post(sut.apiAddress() + "/views/search/sync")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList("results." + q.id() + ".search_types." + messageList.id() + ".messages.message.message", String.class);

        assertThat(j).containsExactly("Boom");
    }
}
