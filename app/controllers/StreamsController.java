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
import models.StreamService;
import models.api.results.StreamsResult;
import play.mvc.Result;

import java.io.IOException;

public class StreamsController extends AuthenticatedController {

    @Inject
    private StreamService streamService;

	public Result index() {
		try {
			StreamsResult streams = streamService.allEnabled();

			return ok(views.html.streams.index.render(currentUser(), streams));
		} catch (IOException e) {
			return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "Could not fetch streams. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
        catch (Exception e) {
            return internalServerError();
        }
	}

    public Result newStream() {
        return ok(views.html.streams.new_stream.render(currentUser()));
    }
	
}
