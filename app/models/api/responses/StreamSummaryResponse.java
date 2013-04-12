package models.api.responses;

import com.google.gson.annotations.SerializedName;

public class StreamSummaryResponse {

	public String id;
	public String title;
	
	@SerializedName("created_at")
	public String createdAt;
	
	@SerializedName("creator_user_id")
	public String creatorUserId;
	
	// public List<StreamRuleSummary> rules;
	
}
