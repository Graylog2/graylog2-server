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
package org.graylog2.restclient.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ApiRequestBuilder;
import org.graylog2.restclient.models.alerts.Alert;
import org.graylog2.restclient.models.alerts.AlertConditionService;
import org.graylog2.restclient.models.api.requests.alerts.CreateAlertConditionRequest;
import org.graylog2.restclient.models.api.responses.alerts.AlertSummaryResponse;
import org.graylog2.restclient.models.api.responses.alerts.AlertsResponse;
import org.graylog2.restclient.models.api.responses.alerts.CheckConditionResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamRuleSummaryResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamSummaryResponse;
import org.graylog2.restclient.models.api.responses.streams.StreamThroughputResponse;
import org.graylog2.restroutes.generated.routes;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Stream {
    private final StreamService streamService;

    public interface Factory {
        Stream fromSummaryResponse(StreamSummaryResponse ssr);
    }

    private final ApiClient api;

	private final String id;
    private final String title;
    private final String description;
    private final String creatorUserId;
    private final String createdAt;
    private final String contentPack;
    private final List<StreamRule> streamRules;
    private final Boolean disabled;
    private final String matchingType;

    private final UserService userService;
    private final AlertConditionService alertConditionService;
    private final StreamRule.Factory streamRuleFactory;

    private final List<String> userAlertReceivers;
    private final List<String> emailAlertReceivers;

    private AlertsResponse alertsResponse;

	@AssistedInject
    private Stream(ApiClient api,
                   UserService userService,
                   AlertConditionService alertConditionService,
                   StreamRule.Factory streamRuleFactory,
                   StreamService streamService,
                   @Assisted StreamSummaryResponse ssr) {
        this.streamService = streamService;
        this.id = ssr.id;
        this.title = ssr.title;
        this.description = ssr.description;
        this.creatorUserId = ssr.creatorUserId;
        this.createdAt = ssr.createdAt;
        this.contentPack = ssr.contentPack;

        this.streamRules = Lists.newArrayList();

        this.disabled = ssr.disabled;

        this.api = api;
        this.userService = userService;
        this.alertConditionService = alertConditionService;
        this.streamRuleFactory = streamRuleFactory;

        if (ssr.alertReceivers != null) {
            if (ssr.alertReceivers.containsKey("users") && ssr.alertReceivers.get("users") != null) {
                userAlertReceivers = ssr.alertReceivers.get("users");
            } else {
                userAlertReceivers = Lists.newArrayList();
            }

            if (ssr.alertReceivers.containsKey("emails") && ssr.alertReceivers.get("emails") != null) {
                emailAlertReceivers = ssr.alertReceivers.get("emails");
            } else {
                emailAlertReceivers = Lists.newArrayList();
            }
        } else {
            userAlertReceivers = Lists.newArrayList();
            emailAlertReceivers = Lists.newArrayList();
        }

        for (StreamRuleSummaryResponse streamRuleSummaryResponse : ssr.streamRules) {
            streamRules.add(streamRuleFactory.fromSummaryResponse(streamRuleSummaryResponse));
        }

        this.matchingType = ssr.matchingType;
	}

    public void addAlertCondition(CreateAlertConditionRequest r) throws APIException, IOException {
        alertConditionService.create(this, r);
    }

    public void addAlertReceiver(User user) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().addReceiver(getId()))
                .queryParam("entity", user.getName())
                .queryParam("type", "users")
                .expect(201)
                .execute();
    }

    public void addAlertReceiver(String email) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().addReceiver(getId()))
                .queryParam("entity", email)
                .queryParam("type", "emails")
                .expect(201)
                .execute();
    }

    public void removeAlertReceiver(User user) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().removeReceiver(getId()))
                .queryParam("entity", user.getName())
                .queryParam("type", "users")
                .expect(204)
                .execute();
    }

    public void removeAlertReceiver(String email) throws APIException, IOException {
        api.path(routes.StreamAlertReceiverResource().removeReceiver(getId()))
                .queryParam("entity", email)
                .queryParam("type", "emails")
                .expect(204)
                .execute();
    }

    @JsonIgnore
    public List<Alert> getAlerts() throws APIException, IOException {
        return getAlertsSince(0);
    }

    public List<Alert> getAlertsSince(int since) throws APIException, IOException {
        List<Alert> alerts = Lists.newArrayList();

        for (AlertSummaryResponse alert : getAlertsInformation(since).alerts) {
            alerts.add(new Alert(alert));
        }

        return alerts;
    }

    @JsonIgnore
    public Long getTotalAlerts() throws APIException, IOException {
        return getAlertsInformation(0).total;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    @JsonIgnore
    public User getCreatorUser() {
        return userService.load(this.creatorUserId);
    }

    public DateTime getCreatedAt() {
        return DateTime.parse(createdAt);
    }

    public String getContentPack() {
        return contentPack;
    }

    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public Boolean getDisabled() {
        return (disabled != null && disabled);
    }

    private AlertsResponse getAlertsInformation(int since) throws APIException, IOException {
        if (alertsResponse == null) {
            ApiRequestBuilder<AlertsResponse> call = api.path(routes.StreamAlertResource().list(getId()), AlertsResponse.class);

            if (since > 0) {
                call.queryParam("since", since);
            }

            alertsResponse = call.execute();
        }

        return alertsResponse;
    }

    @JsonIgnore
    public int getActiveAlerts() throws APIException, IOException {
        CheckConditionResponse response = streamService.activeAlerts(this.getId());
        int size = (response.results == null ? 0 : response.results.size());

        return size;
    }

    @JsonIgnore
    public long getThroughput() throws APIException, IOException {
        long result = 0;
        final Map<Node, StreamThroughputResponse> throughputResponses = api.path(routes.StreamResource().oneStreamThroughput(getId()), StreamThroughputResponse.class)
                .fromAllNodes()
                .expect(200, 404)
                .executeOnAll();

        for (StreamThroughputResponse throughputResponse : throughputResponses.values())
            if (throughputResponse != null)
                result += throughputResponse.throughput;
        return result;
    }


    @JsonIgnore
    public List<String> getUserAlertReceivers() {
        return userAlertReceivers;
    }

    @JsonIgnore
    public List<String> getEmailAlertReceivers() {
        return emailAlertReceivers;
    }

    public String getMatchingType() {
        return matchingType;
    }
}
