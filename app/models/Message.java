package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.GetMessageResponse;
import models.api.responses.MessageAnalyzeResponse;
import models.api.results.MessageAnalyzeResult;
import models.api.results.MessageResult;

import java.io.IOException;
import java.util.ArrayList;

public class Message {

	public static MessageResult get(String index, String id) throws IOException, APIException {
        final GetMessageResponse r = ApiClient.get(GetMessageResponse.class)
                .path("/messages/{0}/{1}", index, id)
                .execute();
		return new MessageResult(r.message, r.index);
	}
	
	public static MessageAnalyzeResult analyze(String index, String what) throws IOException, APIException {
		if (what == null || what.isEmpty()) {
			return new MessageAnalyzeResult(new ArrayList<String>());
		}

        MessageAnalyzeResponse r = ApiClient.get(MessageAnalyzeResponse.class)
                .path("/messages/{0}/analyze", index)
                .queryParam("string", what)
                .execute();
		return new MessageAnalyzeResult(r.tokens);
	}
	
}
