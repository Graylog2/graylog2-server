package models;

import models.api.responses.MessageResponse;

public class MessageResult {

	private final MessageResponse message;
	private final String index;
	
	public MessageResult(MessageResponse message, String index) {
		this.message = message;
		this.index = index;
	}
	
	public MessageResponse getMessage() {
		return message;
	}
	
	public String getIndex() {
		return index;
	}
	
}
