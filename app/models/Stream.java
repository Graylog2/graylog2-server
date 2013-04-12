package models;

import java.io.IOException;
import java.net.URL;

import lib.APIException;
import lib.Api;
import models.api.responses.GetStreamsResponse;
import models.api.responses.StreamSummaryResponse;
import models.api.results.StreamsResult;

public class Stream {
	
	private final String id;

	public Stream(StreamSummaryResponse ssr) {
		this.id = ssr.id;
	}

	public static StreamsResult allEnabled() throws IOException, APIException {
		URL url = Api.buildTarget("streams");
		GetStreamsResponse r = Api.get(url, new GetStreamsResponse());
		
		return new StreamsResult(r.total, r.streams);
	}
	
}
