package models.api.responses;

import com.google.gson.annotations.SerializedName;

public class MessageResponse {

	@SerializedName("_id") 
	public String id;
	
	public String source;
	public String message;
	public String timestamp;
	
}
