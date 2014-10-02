/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
