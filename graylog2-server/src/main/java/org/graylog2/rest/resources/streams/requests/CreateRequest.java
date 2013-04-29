package org.graylog2.rest.resources.streams.requests;

import com.google.gson.annotations.SerializedName;

public class CreateRequest {

	public String title;
	
	@SerializedName("creator_user_id")
	public String creatorUserId;
	
}
