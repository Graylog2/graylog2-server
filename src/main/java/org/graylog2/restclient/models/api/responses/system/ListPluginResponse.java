package org.graylog2.restclient.models.api.responses.system;

import org.graylog2.restclient.models.Plugin;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ListPluginResponse {
    public int total;
    public List<Plugin> plugins;
}
