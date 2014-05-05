/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package models.alerts;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.Stream;
import models.api.requests.alerts.CreateAlertConditionRequest;
import models.api.responses.alerts.AlertConditionSummaryResponse;
import models.api.responses.alerts.AlertConditionsResponse;

import java.io.IOException;
import java.util.List;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertConditionService {

    private final ApiClient api;
    private final AlertCondition.Factory alertConditionFactory;

    @Inject
    private AlertConditionService(ApiClient api, AlertCondition.Factory factory) {
        this.api = api;
        this.alertConditionFactory = factory;
    }

    public List<AlertCondition> allOfStream(Stream stream) throws APIException, IOException {
        List<AlertCondition> conditions = Lists.newArrayList();

        AlertConditionsResponse response = api.get(AlertConditionsResponse.class)
                .path("/streams/{0}/alerts/conditions", stream.getId())
                .execute();

        for (AlertConditionSummaryResponse c : response.conditions) {
            conditions.add(alertConditionFactory.fromSummaryResponse(c));
        }

        return conditions;
    }

    public void delete(Stream stream, String conditionId) throws APIException, IOException {
        api.delete().path("/streams/{0}/alerts/conditions/{1}", stream.getId(), conditionId).expect(204).execute();
    }

    public void create(Stream stream, CreateAlertConditionRequest r) throws APIException, IOException {
        api.post().body(r).path("/streams/{0}/alerts/conditions", stream.getId()).expect(201).execute();
    }

}
