package models.api.results;

import java.util.List;

import com.google.common.collect.Lists;

import models.Stream;
import models.api.responses.StreamSummaryResponse;

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

}
