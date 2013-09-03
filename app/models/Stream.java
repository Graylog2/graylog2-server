package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.GetStreamsResponse;
import models.api.responses.StreamSummaryResponse;
import models.api.results.StreamsResult;

import java.io.IOException;

public class Stream {
	
	private final String id;

	public Stream(StreamSummaryResponse ssr) {
		this.id = ssr.id;
	}

	public static StreamsResult allEnabled() throws IOException, APIException {
		GetStreamsResponse r = ApiClient.get(GetStreamsResponse.class).path("streams").execute();
		
		return new StreamsResult(r.total, r.streams);
	}
	
}
