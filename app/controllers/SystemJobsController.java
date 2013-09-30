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

import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.ClusterService;
import models.SystemJob;
import play.Logger;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemJobsController extends AuthenticatedController {

    @Inject
    private ClusterService clusterService;

    public Result trigger() {
        Http.RequestBody body = request().body();

        if (body.asFormUrlEncoded().get("job") == null) {
            Logger.warn("No job name provided.");
            return forbidden();
        }

        try {
            final String jobType = body.asFormUrlEncoded().get("job")[0];
            clusterService.triggerSystemJob(SystemJob.Type.fromString(jobType), currentUser());
            return redirect(routes.SystemController.index(1));
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            String message = "Could not trigger system job. We expected HTTP 202, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

    }

}
