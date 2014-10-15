/**
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
package org.graylog2.restclient.models.alerts;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.api.requests.alerts.CreateAlertConditionRequest;
import org.graylog2.restclient.models.api.responses.alerts.AlertConditionSummaryResponse;
import org.graylog2.restclient.models.api.responses.alerts.AlertConditionsResponse;
import org.graylog2.restroutes.generated.routes;

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

        AlertConditionsResponse response = api.path(routes.StreamAlertConditionResource().list(stream.getId()), AlertConditionsResponse.class)
                .execute();

        for (AlertConditionSummaryResponse c : response.conditions) {
            conditions.add(alertConditionFactory.fromSummaryResponse(c));
        }

        return conditions;
    }

    public void delete(Stream stream, String conditionId) throws APIException, IOException {
        api.path(routes.StreamAlertConditionResource().delete(stream.getId(), conditionId)).expect(204).execute();
    }

    public void create(Stream stream, CreateAlertConditionRequest r) throws APIException, IOException {
        api.path(routes.StreamAlertConditionResource().create(stream.getId())).body(r).expect(201).execute();
    }

    public void update(Stream stream, String conditionId, CreateAlertConditionRequest r) throws APIException, IOException {
        api.path(routes.StreamAlertConditionResource().update(stream.getId(), conditionId)).body(r).expect(204).execute();
    }
}
