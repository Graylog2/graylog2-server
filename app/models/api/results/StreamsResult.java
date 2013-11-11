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
package models.api.results;

import java.util.List;

import com.google.common.collect.Lists;

import models.Stream;
import models.api.responses.streams.StreamSummaryResponse;

public class StreamsResult {

	private final int total;
	private final List<StreamSummaryResponse> streams;
	
	public StreamsResult(int total, List<StreamSummaryResponse> streams) {
		this.total = total;
		this.streams = streams;
	}
	
	public int getTotal() {
		return total;
	}
	
	public List<Stream> getStreams() {
		List<Stream> streams = Lists.newArrayList();
		
		for (StreamSummaryResponse ssr : this.streams) {
			streams.add(new Stream(ssr));
		}
		
		return streams;
	}

    public static StreamsResult mergeResults(List<StreamsResult> streamsResults) {
        int total = 0;
        List<StreamSummaryResponse> streams = Lists.newArrayList();

        for (StreamsResult streamsResult : streamsResults) {
            total += streamsResult.total;
            streams.addAll(streamsResult.streams);
        }

        return new StreamsResult(total, streams);
    }
}
