package models.api.responses.system;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserResponse {

	public String username;

	@SerializedName("full_name")
	public String fullName;

	public String id;

    public String email;

	public List<String> permissions;
}
