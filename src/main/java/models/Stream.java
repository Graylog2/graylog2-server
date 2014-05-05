/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package models;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import lib.APIException;
import lib.ApiClient;
import lib.ApiRequestBuilder;
import models.alerts.Alert;
import models.alerts.AlertConditionService;
import models.api.requests.alerts.CreateAlertConditionRequest;
import models.api.responses.alerts.AlertSummaryResponse;
import models.api.responses.alerts.AlertsResponse;
import models.api.responses.streams.StreamRuleSummaryResponse;
import models.api.responses.streams.StreamSummaryResponse;
import models.api.responses.streams.StreamThroughputResponse;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class Stream {

    public interface Factory {
        public Stream fromSummaryResponse(StreamSummaryResponse ssr);
    }

    private final ApiClient api;
	
	private final String id;
    private final String title;
    private final String description;
    private final String creatorUserId;
    private final String createdAt;
    private final List<StreamRule> streamRules;
    private final Boolean disabled;

    private final UserService userService;
    private final AlertConditionService alertConditionService;
    private final StreamRule.Factory streamRuleFactory;

    private final List<String> userAlertReceivers;
    private final List<String> emailAlertReceivers;

    private AlertsResponse alertsResponse;

	@AssistedInject
    private Stream(ApiClient api, UserService userService, AlertConditionService alertConditionService, StreamRule.Factory streamRuleFactory, @Assisted StreamSummaryResponse ssr) {
		this.id = ssr.id;
        this.title = ssr.title;
        this.description = ssr.description;
        this.creatorUserId = ssr.creatorUserId;
        this.createdAt = ssr.createdAt;

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
	}

    public void addAlertCondition(CreateAlertConditionRequest r) throws APIException, IOException {
        alertConditionService.create(this, r);
    }

    public void addAlertReceiver(User user) throws APIException, IOException {
        api.post().path("/streams/{0}/alerts/receivers", getId())
                .queryParam("entity", user.getName())
                .queryParam("type", "users")
                .expect(201)
                .execute();
    }

    public void addAlertReceiver(String email) throws APIException, IOException {
        api.post().path("/streams/{0}/alerts/receivers", getId())
                .queryParam("entity", email)
                .queryParam("type", "emails")
                .expect(201)
                .execute();
    }

    public void removeAlertReceiver(User user) throws APIException, IOException {
        api.delete().path("/streams/{0}/alerts/receivers", getId())
                .queryParam("entity", user.getName())
                .queryParam("type", "users")
                .expect(204)
                .execute();
    }

    public void removeAlertReceiver(String email) throws APIException, IOException {
        api.delete().path("/streams/{0}/alerts/receivers", getId())
                .queryParam("entity", email)
                .queryParam("type", "emails")
                .expect(204)
                .execute();
    }

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

    public User getCreatorUser() {
        return userService.load(this.creatorUserId);
    }

    public DateTime getCreatedAt() {
        return DateTime.parse(createdAt);
    }

    public List<StreamRule> getStreamRules() {
        return streamRules;
    }

    public Boolean getDisabled() {
        return (disabled != null && disabled);
    }

    private final AlertsResponse getAlertsInformation(int since) throws APIException, IOException {
        if (alertsResponse == null) {
            ApiRequestBuilder<AlertsResponse> call = api.get(AlertsResponse.class)
                    .path("/streams/{0}/alerts", getId());

            if (since > 0) {
                call.queryParam("since", since);
            }

            alertsResponse = call.execute();
        }

        return alertsResponse;
    }

    public long getThroughput() throws APIException, IOException {
        final StreamThroughputResponse throughputResponse = api.get(StreamThroughputResponse.class)
                .path("/streams/{0}/throughput", getId())
                .expect(200, 404)
                .execute();

        if (throughputResponse == null) {
            return 0L;
        }
        return throughputResponse.throughput;
    }


    public List<String> getUserAlertReceivers() {
        return userAlertReceivers;
    }

    public List<String> getEmailAlertReceivers() {
        return emailAlertReceivers;
    }

}
