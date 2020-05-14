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
package org.graylog.testing.backenddriver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;
import io.restassured.specification.RequestSpecification;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;

import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * WIP. This class illustrates how we could reuse common functionality for integration tests.
 * It might make sense to configure it with @link io.restassured.specification.RequestSpecification
 * and whatever else it might need in the future and make it injectable into the test class via
 *
 * @link org.graylog.testing.completebackend.GraylogBackendExtension
 * We should do that later, if we find that implementing more functionality here is useful and feasible.
 */
public class SearchDriver {

    /**
     * @param requestSpec @see io.restassured.specification.RequestSpecification
     * @return all messages' "message" field as List<String>
     */
    public static List<String> searchAllMessages(RequestSpecification requestSpec) {
        String queryId = "query-id";
        String messageListId = "message-list-id";

        String body = allMessagesJson(queryId, messageListId);

        return given()
                .spec(requestSpec)
                .when()
                .body(body)
                .post("/views/search/sync")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath().getList(allMessagesJsonPath(queryId, messageListId), String.class);
    }

    private static String allMessagesJson(String queryId, String messageListId) {
        MessageList messageList = MessageList.builder().id(messageListId).build();
        Query q = Query.builder()
                .id(queryId)
                .query(ElasticsearchQueryString.builder().queryString("").build())
                .timerange(allMessagesTimeRange())
                .searchTypes(ImmutableSet.of(messageList))
                .build();
        Search s = Search.builder().queries(ImmutableSet.of(q)).build();

        return toJsonString(s);
    }

    private static AbsoluteRange allMessagesTimeRange() {
        try {
            return AbsoluteRange.create("2010-01-01T00:00:00.0Z", "2050-01-01T00:00:00.0Z");
        } catch (InvalidRangeParametersException e) {
            throw new RuntimeException("boo hoo", e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static String allMessagesJsonPath(String queryId, String messageListId) {
        return "results." + queryId + ".search_types." + messageListId + ".messages.message.message";
    }

    private static String toJsonString(Search s) {
        try {
            return new ObjectMapperProvider().get().writeValueAsString(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize Search", e);
        }
    }
}
