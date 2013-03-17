package lib;

import play.Play;

public class Configuration {
	
	public static String getServerRestUri() {
		String uri = Play.application().configuration().getString("graylog2-server.uri");
		
		if (uri != null && !uri.endsWith("/")) {
			uri += "/";
		}
		
		return uri;
	}
	
}
