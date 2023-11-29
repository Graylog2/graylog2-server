package org.graylog2.bootstrap.preflight.web.resources.model;

import java.net.URI;
import java.util.List;

public record RemoteReindexRequest(URI hostname, String user, String password, List<String> indices) {
}
