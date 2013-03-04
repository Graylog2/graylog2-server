package models;

import play.data.validation.Constraints.Required;

public class LoginRequest {

	@Required public String username;
	@Required public String password;
	
}
