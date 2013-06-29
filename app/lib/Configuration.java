package lib;

import com.google.common.collect.Lists;
import play.Play;

import java.util.List;

public class Configuration {
	
	public static List<String> getServerRestUris() {
		String urisString = Play.application().configuration().getString("graylog2-server.uris");

        List<String> uris = Lists.newArrayList();

        // TODO make this more robust and fault tolerant. just a quick hack to get it working for now.
        for (String uri : urisString.split(",")) {
            if (uri != null && !uri.endsWith("/")) {
                uri += "/";
            }

            uris.add(uri);
        }
		
		return uris;
	}
	
}
