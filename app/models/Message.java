package models;

import java.io.IOException;
import java.net.URL;

import lib.APIException;
import lib.Api;
import models.api.responses.GetMessageResponse;

public class Message {

	public static MessageResult get(String index, String id) throws IOException, APIException {
		URL url = Api.buildTarget("messages/" + index + "/" + id);

		GetMessageResponse r = Api.get(url, new GetMessageResponse());
		return new MessageResult(r.message, r.index);
	}
	
}
