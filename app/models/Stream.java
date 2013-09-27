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
