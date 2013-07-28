package models;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import lib.APIException;
import lib.Api;
import models.api.responses.GetMessageResponse;
import models.api.responses.MessageAnalyzeResponse;
import models.api.results.MessageAnalyzeResult;
import models.api.results.MessageResult;

public class Message {

	public static MessageResult get(String index, String id) throws IOException, APIException {
		String resource = "messages/" + index + "/" + id;
		
		GetMessageResponse r = Api.get(resource, GetMessageResponse.class);
		return new MessageResult(r.message, r.index);
	}
	
	public static MessageAnalyzeResult analyze(String index, String what) throws IOException, APIException {
		if (what == null || what.isEmpty()) {
			return new MessageAnalyzeResult(new ArrayList<String>());
		}
		
		String resource = "messages/" + index + "/analyze?string=" + Api.urlEncode(what);
		
		MessageAnalyzeResponse r = Api.get(resource, MessageAnalyzeResponse.class);
		return new MessageAnalyzeResult(r.tokens);
	}
	
}
