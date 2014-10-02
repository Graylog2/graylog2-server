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
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.plugin.configuration.RequestedConfigurationField;
import org.graylog2.restclient.models.api.requests.alarmcallbacks.CreateAlarmCallbackRequest;
import org.graylog2.restclient.models.api.responses.alarmcallbacks.*;
import org.graylog2.restroutes.generated.AlarmCallbackResource;
import org.graylog2.restroutes.generated.routes;
import play.mvc.Http;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackService {
    private final ApiClient apiClient;
    private final AlarmCallback.Factory alarmCallbackFactory;
    private final AlarmCallbackResource resource = routes.AlarmCallbackResource();

    @Inject
    public AlarmCallbackService(ApiClient apiClient,
                                AlarmCallback.Factory alarmCallbackFactory) {
        this.apiClient = apiClient;
        this.alarmCallbackFactory = alarmCallbackFactory;
    }

    public List<AlarmCallback> all(String streamId) throws IOException, APIException {
        GetAlarmCallbacksResponse response = apiClient.path(resource.get(streamId), GetAlarmCallbacksResponse.class)
                .expect(Http.Status.OK).execute();

        List<AlarmCallback> result = Lists.newArrayList();
        for (AlarmCallbackSummaryResponse callbackResponse : response.alarmcallbacks) {
            result.add(alarmCallbackFactory.fromSummaryResponse(streamId, callbackResponse));
        }

        return result;
    }

    public AlarmCallback get(String streamId, String alarmCallbackId) throws IOException, APIException {
        AlarmCallbackSummaryResponse response = apiClient.path(resource.get(streamId, alarmCallbackId), AlarmCallbackSummaryResponse.class)
                .expect(Http.Status.OK).execute();

        return alarmCallbackFactory.fromSummaryResponse(streamId, response);
    }

    public CreateAlarmCallbackResponse create(String streamId, CreateAlarmCallbackRequest request) throws IOException, APIException {
        return apiClient.path(resource.create(streamId), CreateAlarmCallbackResponse.class).body(request).expect(Http.Status.CREATED).execute();
    }

    public Map<String, GetSingleAvailableAlarmCallbackResponse> available(String streamId) throws IOException, APIException {
        GetAvailableAlarmCallbacksResponse response = apiClient.path(resource.available(streamId), GetAvailableAlarmCallbacksResponse.class)
                .expect(Http.Status.OK).execute();

        return response.types;
    }

    public void delete(String streamId, String alarmCallbackId) throws IOException, APIException {
        apiClient.path(resource.delete(streamId, alarmCallbackId)).expect(Http.Status.NO_CONTENT).execute();
    }
}
