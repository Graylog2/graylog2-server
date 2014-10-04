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
