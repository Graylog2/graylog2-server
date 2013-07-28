package lib;

import com.google.common.collect.Lists;
import play.Logger;
import play.Play;

import java.util.List;

public class Configuration {

    // Variables that can be overriden. (for example in tests)
    private static String graylog2ServerUris = Play.application().configuration().getString("graylog2-server.uris");

	public static List<String> getServerRestUris() {
        List<String> uris = Lists.newArrayList();

        // TODO make this more robust and fault tolerant. just a quick hack to get it working for now.
        for (String uri : graylog2ServerUris.split(",")) {
            if (uri != null && !uri.endsWith("/")) {
                uri += "/";
            }

            uris.add(uri);
        }
		
		return uris;
	}

    public static void setServerRestUris(String URIs) {
        graylog2ServerUris = URIs;
        Logger.info("graylog2-server.uris overridden with <" + URIs + ">.");
    }
	
}
