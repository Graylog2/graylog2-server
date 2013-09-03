package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.DateHistogramResponse;
import models.api.results.DateHistogramResult;

import java.io.IOException;

public class MessageCountHistogram {

	private final String interval;
	private final int timerange;
	
	public MessageCountHistogram(String interval, int timerange) {
		this.interval = interval;
		this.timerange = timerange;
	}
	
	public DateHistogramResult histogram() throws IOException, APIException {
        DateHistogramResponse response = ApiClient.get(DateHistogramResponse.class)
                .path("/count/histogram")
                .queryParam("interval", interval)
                .queryParam("timerange", timerange)
                .execute();
		return new DateHistogramResult("match_all", response.time, response.interval, response.results);
	}
	
}
