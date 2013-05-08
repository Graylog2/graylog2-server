package controllers;

import java.io.IOException;
import java.util.List;

import lib.APIException;
import lib.Api;
import play.mvc.Result;
import models.Stream;
import models.api.results.StreamsResult;

public class StreamsController extends AuthenticatedController {

	public static Result index() {
		try {
			StreamsResult streams = Stream.allEnabled();

			return ok(views.html.streams.index.render(currentUser(), streams));
		} catch (IOException e) {
			return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "Could not fetch streams. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
	}

    public static Result newStream() {
        return ok(views.html.streams.new_stream.render(currentUser()));
    }
	
}
