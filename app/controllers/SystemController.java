/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package controllers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import controllers.AuthenticatedController;
import lib.APIException;
import lib.Api;
import models.*;
import models.api.responses.system.ServerThroughputResponse;
import play.Logger;
import play.mvc.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemController extends AuthenticatedController {

    public static Result index() {
        try {
            List<Notification> notifications = Notification.all();
            List<SystemJob> systemJobs = SystemJob.all();
            List<SystemMessage> systemMessages = SystemMessage.all();
            ESClusterHealth clusterHealth = ESClusterHealth.get();

            return ok(views.html.system.index.render(currentUser(), systemJobs, clusterHealth, systemMessages, notifications));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

    public static Result nodes() {
        try {
            List<ServerJVMStats> serverJvmStats = ServerJVMStats.get();
            Map<String, BufferInfo> bufferInfo = Maps.newHashMap();

            // Ask every node for buffer info.
            for(Node node : Node.all()) {
                bufferInfo.put(node.getNodeId(), BufferInfo.ofNode(node));
            }

            return ok(views.html.system.nodes.render(currentUser(), serverJvmStats, bufferInfo));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }


}
