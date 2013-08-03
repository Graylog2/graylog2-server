package controllers;

import java.io.IOException;

import com.google.gson.Gson;

import lib.APIException;
import lib.Api;
import models.Input;
import models.Message;
import models.Node;
import models.api.results.MessageAnalyzeResult;
import models.api.results.MessageResult;
import play.Logger;
import play.mvc.*;

public class MessagesController extends AuthenticatedController {

	public static Result asPartial(String index, String id) {
		try {
			MessageResult message = Message.get(index, id);

            Input sourceInput = null;
            Node sourceNode = null;

            try {
                sourceNode = Node.fromId(message.getSourceNodeId());
            } catch(Exception e) {
                Logger.warn("Could not derive source node from message <" + id + ">.", e);
            }

            if (sourceNode != null) {
                try {
                    sourceInput = sourceNode.getInput(message.getSourceInputId());
                } catch(Exception e) {
                    Logger.warn("Could not derive source input from message <" + id + ">.", e);
                }
            }

			return ok(views.html.messages.show_as_partial.render(message, sourceInput, sourceNode));
		} catch (IOException e) {
			return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "Could not get message. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
	}
	
	public static Result analyze(String index, String id, String field) {
		try {
			MessageResult message = Message.get(index, id);
			
			String analyzeField = (String) message.getFields().get(field);
			if (analyzeField == null || analyzeField.isEmpty()) {
				throw new APIException(404, "Message does not have requested field.");
			}
			
			MessageAnalyzeResult result = Message.analyze(index, analyzeField);
			return ok(new Gson().toJson(result.getTokens())).as("application/json");
		} catch (IOException e) {
			return status(504, views.html.errors.error.render(Api.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(504, views.html.errors.error.render(message, e, request()));
		}
	}
	
}
