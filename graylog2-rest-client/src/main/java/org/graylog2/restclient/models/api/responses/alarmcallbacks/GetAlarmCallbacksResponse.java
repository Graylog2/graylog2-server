package org.graylog2.restclient.models.api.responses.alarmcallbacks;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GetAlarmCallbacksResponse {
    public Long total;
    public List<AlarmCallbackSummaryResponse> alarmcallbacks;
}
