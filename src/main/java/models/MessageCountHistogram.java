/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package models;

import lib.APIException;
import lib.ApiClient;
import models.api.responses.DateHistogramResponse;
import models.api.results.DateHistogramResult;

import java.io.IOException;

public class MessageCountHistogram {

    private final ApiClient api;
    private final String interval;
	private final int timerange;
	
	public MessageCountHistogram(ApiClient api, String interval, int timerange) {
        this.api = api;
        this.interval = interval;
		this.timerange = timerange;
	}
	
	public DateHistogramResult histogram() throws IOException, APIException {
        DateHistogramResponse response = api.get(DateHistogramResponse.class)
                .path("/count/histogram")
                .queryParam("interval", interval)
                .queryParam("timerange", timerange)
                .execute();
		return new DateHistogramResult("match_all", response.time, response.interval, response.results);
	}
	
}
