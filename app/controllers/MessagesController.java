package controllers;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.*;
import models.api.results.MessageAnalyzeResult;
import models.api.results.MessageResult;
import play.Logger;
import play.mvc.Result;

import java.io.IOException;
import java.util.Map;

public class MessagesController extends AuthenticatedController {

    @Inject
    private NodeService nodeService;

    @Inject
    private MessageLoader messageLoader;

    public Result single(String index, String id) {
        try {
            MessageResult message = messageLoader.get(index, id);

            Map<String, Object> result = Maps.newHashMap();
            result.put("id", message.getId());
            result.put("fields", message.getFields());

            return ok(new Gson().toJson(result)).as("application/json");
        } catch (IOException e) {
            return status(500);
        } catch (APIException e) {
            return status(e.getHttpCode());
        }
    }

	public Result singleAsPartial(String index, String id) {
		try {
            MessageResult message = FieldMapper.run(messageLoader.get(index, id));
            Node sourceNode = getSourceNode(message);

            return ok(views.html.messages.show_as_partial.render(message, getSourceInput(sourceNode, message), sourceNode));
		} catch (IOException e) {
			return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "Could not get message. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(500, views.html.errors.error.render(message, e, request()));
		}
	}
	
	public Result analyze(String index, String id, String field) {
		try {
			MessageResult message = messageLoader.get(index, id);
			
			String analyzeField = (String) message.getFields().get(field);
			if (analyzeField == null || analyzeField.isEmpty()) {
				throw new APIException(404, "Message does not have requested field.");
			}
			
			MessageAnalyzeResult result = messageLoader.analyze(index, analyzeField);
			return ok(new Gson().toJson(result.getTokens())).as("application/json");
		} catch (IOException e) {
			return status(500, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
		} catch (APIException e) {
			String message = "There was a problem with your search. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
			return status(500, views.html.errors.error.render(message, e, request()));
		}
	}

    private Node getSourceNode(MessageResult m) {
        try {
            return nodeService.loadNode(m.getSourceNodeId());
        } catch(Exception e) {
            Logger.warn("Could not derive source node from message <" + m.getId() + ">.", e);
        }

        return null;
    }

    private static Input getSourceInput(Node node, MessageResult m) {
        if (node != null) {
            try {
                return node.getInput(m.getSourceInputId());
            } catch(Exception e) {
                Logger.warn("Could not derive source input from message <" + m.getId() + ">.", e);
            }
        }

        return null;
    }
	
}
