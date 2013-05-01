package org.graylog2.rest.resources.streams.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateRequest {

	public String title;
	
	@JsonProperty("creator_user_id")
	public String creatorUserId;
}
