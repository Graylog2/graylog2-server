package controllers;

import java.io.IOException;

import lib.APIException;
import models.Message;
import models.MessageResult;
import play.mvc.*;

public class MessagesController extends AuthenticatedController {

	public static Result asPartial(String index, String id) {
		try {
			MessageResult message = Message.get(index, id);
			return ok(views.html.messages.show_as_partial.render(message.getMessage(), message.getIndex()));
		} catch (IOException e) {
			String message = "Could not connect to graylog2-server. Please make sure that it is running and you " +
					"configured the correct REST URI.";
			return status(504, views.html.errors.error.render(message, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
		
		
	}
	
}
