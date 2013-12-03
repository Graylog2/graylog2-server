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
package controllers;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.BreadcrumbList;
import lib.ServerNodes;
import models.*;
import play.mvc.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private ServerNodes serverNodes;

    public Result index(Integer page) {
        try {
            List<Notification> notifications = clusterService.allNotifications();
            List<SystemJob> systemJobs = clusterService.allSystemJobs();
            int totalSystemMessages = clusterService.getNumberOfSystemMessages();
            List<SystemMessage> systemMessages = clusterService.getSystemMessages(page - 1);
            ESClusterHealth clusterHealth = clusterService.getESClusterHealth();

            return ok(views.html.system.index.render(
                    currentUser(),
                    systemJobs,
                    clusterHealth,
                    systemMessages,
                    totalSystemMessages,
                    page,
                    notifications
            ));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

}
