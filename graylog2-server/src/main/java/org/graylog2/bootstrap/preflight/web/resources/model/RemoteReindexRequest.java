package org.graylog2.bootstrap.preflight.web.resources.model;

import java.util.List;

public record RemoteReindexRequest(String hostname, String user, String password, List<String> indices) {
}
