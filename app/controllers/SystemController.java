/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.google.common.collect.Lists;
import lib.notifications.NotificationType;
import lib.notifications.NotificationTypeFactory;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.*;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static lib.security.RestPermissions.*;
import static views.helpers.Permissions.isPermitted;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemController extends AuthenticatedController {

    private final NodeService nodeService;
    private final ClusterService clusterService;
    private final ServerNodes serverNodes;
    private final NotificationTypeFactory notificationTypeFactory;

    @Inject
    public SystemController(NodeService nodeService, ClusterService clusterService, ServerNodes serverNodes, NotificationTypeFactory notificationTypeFactory) {
        this.nodeService = nodeService;
        this.clusterService = clusterService;
        this.serverNodes = serverNodes;
        this.notificationTypeFactory = notificationTypeFactory;
    }

    public Result index(Integer page) {
        try {
            if (page == 0) {
                page = 1;
            }

            List<NotificationType> notifications = Lists.newArrayList();
            if (isPermitted(NOTIFICATIONS_READ)) {
                for (Notification notification : clusterService.allNotifications())
                    notifications.add(notificationTypeFactory.get(notification));
            }
            List<SystemJob> systemJobs = isPermitted(SYSTEMJOBS_READ) ? clusterService.allSystemJobs() : Collections.<SystemJob>emptyList();
            final Boolean permittedSystemMessages = isPermitted(SYSTEMMESSAGES_READ);
            int totalSystemMessages = permittedSystemMessages ? clusterService.getNumberOfSystemMessages() : 0;
            List<SystemMessage> systemMessages = permittedSystemMessages ? clusterService.getSystemMessages(page) : Collections.<SystemMessage>emptyList();
            ESClusterHealth clusterHealth = isPermitted(INDEXERCLUSTER_READ) ? clusterService.getESClusterHealth() : null;
            long indexFailureCount = isPermitted(INDICES_FAILURES) ? clusterService.getIndexerFailureCountLast24Hours() : -1;
            String masterTimezone = nodeService.loadMasterNode().getTimezone();

            return ok(views.html.system.index.render(
                    currentUser(),
                    systemJobs,
                    clusterHealth,
                    systemMessages,
                    totalSystemMessages,
                    page,
                    notifications,
                    indexFailureCount,
                    masterTimezone
            ));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }
    }

}
