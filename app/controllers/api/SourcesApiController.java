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
package controllers.api;

import com.google.inject.Inject;
import controllers.AuthenticatedController;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.Source;
import org.graylog2.restclient.models.SourcesService;
import play.libs.Json;
import play.mvc.Result;

import java.io.IOException;
import java.util.List;

public class SourcesApiController extends AuthenticatedController {
    @Inject
    private SourcesService sourcesService;

    public Result list(int range) {
        if (range < 0) {
            return status(400, views.html.errors.error.render("Invalid time range for sources list", new Exception(), request()));
        }
        try {
            List<Source> sources = sourcesService.all(range);
            return ok(Json.toJson(sources));
        } catch (IOException e) {
            return internalServerError("io exception");
        } catch (APIException e) {
            if (e.getHttpCode() == 400) {
                // This usually means the field does not have a numeric type. Pass through!
                return badRequest();
            }

            return internalServerError("api exception " + e);
        }
    }
}
