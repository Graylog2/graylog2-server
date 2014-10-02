/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.restclient.models.api.results;

import java.util.List;

import com.google.common.collect.Lists;

import com.google.inject.Inject;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import org.graylog2.restclient.models.api.responses.streams.StreamSummaryResponse;

public class StreamsResult {

	private final int total;
	private final List<StreamSummaryResponse> streams;

    private final Stream.Factory streamFactory;

    @Inject
	public StreamsResult(Stream.Factory streamFactory, int total, List<StreamSummaryResponse> streams) {
		this.total = total;
		this.streams = streams;
        this.streamFactory = streamFactory;
	}

	public int getTotal() {
		return total;
	}
	
	public List<StreamSummaryResponse> getStreams() {
		/*List<Stream> streams = Lists.newArrayList();
		
		for (StreamSummaryResponse ssr : this.streams) {
            streams.add(streamFactory.fromSummaryResponse(ssr));
		}

		return streams;*/
        return streams;
	}

    /*public static StreamsResult mergeResults(List<StreamsResult> streamsResults) {
        int total = 0;
        List<StreamSummaryResponse> streams = Lists.newArrayList();

        for (StreamsResult streamsResult : streamsResults) {
            total += streamsResult.total;
            streams.addAll(streamsResult.streams);
        }

        return new StreamsResult(total, streams);
    }*/
}
