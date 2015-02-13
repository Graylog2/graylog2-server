/**
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
 *
 */
package controllers;

import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import lib.BreadcrumbList;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Radio;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class RadiosController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;

    public Result show(String radioId) {
        try {
            Radio radio = nodeService.loadRadio(radioId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(radio.getShortNodeId(), routes.RadiosController.show(radio.getId()));

            return ok(views.html.system.radios.show.render(currentUser(), bc, radio));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result threadDump(String radioId) {
        try {
            Radio radio = nodeService.loadRadio(radioId);

            BreadcrumbList bc = new BreadcrumbList();
            bc.addCrumb("System", routes.SystemController.index(0));
            bc.addCrumb("Nodes", routes.NodesController.nodes());
            bc.addCrumb(radio.getShortNodeId(), routes.RadiosController.show(radio.getId()));
            bc.addCrumb("Thread dump", routes.RadiosController.threadDump(radioId));

            return ok(views.html.system.threaddump.render(currentUser(), bc, radio, radio.getThreadDump()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not fetch system information. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

    public Result overrideLbStatus(String radioId, String override) {
        try {
            Radio radio = nodeService.loadRadio(radioId);
            radio.overrideLbStatus(override);
            return redirect(routes.NodesController.nodes());
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not set load balancer state. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (NodeService.NodeNotFoundException e) {
            return status(404, views.html.errors.error.render(ApiClient.ERROR_MSG_NODE_NOT_FOUND, e, request()));
        }
    }

}
