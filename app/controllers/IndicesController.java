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

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import lib.BreadcrumbList;
import models.ClusterService;
import models.ESClusterHealth;
import models.IndexService;
import models.api.responses.system.indices.DeflectorConfigResponse;
import models.api.responses.system.indices.DeflectorInformationResponse;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndicesController extends AuthenticatedController {

    @Inject
    private ClusterService clusterService;

    @Inject
    private IndexService indexService;

    public Result index() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Indices", routes.IndicesController.index());

        try {
            ESClusterHealth clusterHealth = clusterService.getESClusterHealth();
            DeflectorInformationResponse deflector = indexService.getDeflectorInfo();
            DeflectorConfigResponse deflectorConfig = indexService.getDeflectorConfig();

            return ok(views.html.system.indices.index.render(
                    currentUser(),
                    bc,
                    indexService.all(),
                    clusterHealth,
                    deflector.currentTarget,
                    deflectorConfig
            ));
        } catch (APIException e) {
            String message = "Could not get indices. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        }
    }

}
