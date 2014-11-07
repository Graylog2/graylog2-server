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
package controllers.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.alerts.Alert;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AlertsApiController extends AuthenticatedController {
    private final StreamService streamService;

    @Inject
    public AlertsApiController(StreamService streamService) {
        this.streamService = streamService;
    }

    public Result allAllowedSince(Integer since) {
        try {
            Map<String, Object> result = Maps.newHashMap();

            List<Map<String, Object>> alerts = Lists.newArrayList();
            for (Alert alert : streamService.allowedAlertsSince(since)) {
                Map<String, Object> alertMap = Maps.newHashMap();

                Stream stream = streamService.get(alert.getStreamId());

                alertMap.put("id", alert.getId());
                alertMap.put("stream_id", alert.getStreamId());
                alertMap.put("stream_name", stream.getTitle());
                alertMap.put("condition_id", alert.getConditionId());
                alertMap.put("parameters", alert.getConditionParameters());
                alertMap.put("triggered_at", alert.getTriggeredAt().getMillis() / 1000);
                alertMap.put("description", alert.getDescription());

                alerts.add(alertMap);
            }

            result.put("alerts", alerts);

            return ok(Json.toJson(result));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            return internalServerError("api exception " + e);
        }
    }
}
