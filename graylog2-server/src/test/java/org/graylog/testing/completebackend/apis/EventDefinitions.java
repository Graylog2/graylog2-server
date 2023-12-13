package org.graylog.testing.completebackend.apis;

import io.restassured.response.ValidatableResponse;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EventDefinitions implements GraylogRestApi {
    private final GraylogApis api;

    public EventDefinitions(GraylogApis api) {
        this.api = api;
    }

    public void deleteDefinition(String notificationID) {
        api.delete("/events/definitions/" + notificationID, 200);
    }

    public String createEventDefinition(String httpNotificationID, List<String> groupByFields) {
        final String body = """
                {
                  "title": "my alert def",
                  "description": "",
                  "priority": 2,
                  "config": {
                    "query": "",
                    "query_parameters": [],
                    "streams": [],
                    "search_within_ms": 5000,
                    "execute_every_ms": 5000,
                    "event_limit": 100,
                    "group_by": [%s],
                    "series": [
                      {
                        "id": "count-",
                        "type": "count"
                      }
                    ],
                    "conditions": {
                      "expression": {
                        "expr": ">",
                        "left": {
                          "expr": "number-ref",
                          "ref": "count-"
                        },
                        "right": {
                          "expr": "number",
                          "value": 0
                        }
                      }
                    },
                    "type": "aggregation-v1"
                  },
                  "field_spec": {},
                  "key_spec": [],
                  "notification_settings": {
                    "grace_period_ms": 300000,
                    "backlog_size": null
                  },
                  "notifications": [{
                    "notification_id": "%s"
                  }],
                  "alert": false
                }
                """;

        final String groupByClause = groupByFields.stream().map(f -> "\"" + f + "\"").collect(Collectors.joining(","));
        final String req = String.format(Locale.ROOT, body, groupByClause, httpNotificationID);

        final ValidatableResponse response = api.post("/events/definitions", req, 200);
        return response.extract().body().jsonPath().getString("id");
    }
}
