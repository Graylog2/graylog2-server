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
		URL url = Api.buildTarget("messages/" + index + "/" + id);
		
		GetMessageResponse r = Api.get(url, GetMessageResponse.class);
		return new MessageResult(r.message, r.index);
	}
	
	public static MessageAnalyzeResult analyze(String index, String what) throws IOException, APIException {
		if (what == null || what.isEmpty()) {
			return new MessageAnalyzeResult(new ArrayList<String>());
		}
		
		URL url = Api.buildTarget("messages/" + index + "/analyze?string=" + Api.urlEncode(what));
		
		MessageAnalyzeResponse r = Api.get(url, MessageAnalyzeResponse.class);
		return new MessageAnalyzeResult(r.tokens);
	}
	
}
