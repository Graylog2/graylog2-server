package models.api.responses.system;

import com.google.gson.annotations.SerializedName;

public class AuthenticationResponse {

	public boolean isAuthorized;

	public String username;

	@SerializedName("full_name")
	public String fullName;

	public String id;
}
