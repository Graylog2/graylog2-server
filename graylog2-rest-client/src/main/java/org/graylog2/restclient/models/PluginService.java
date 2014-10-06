package org.graylog2.restclient.models;

import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.responses.system.ListPluginResponse;
import org.graylog2.restroutes.generated.routes;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PluginService {
    private final ApiClient api;

    @Inject
    public PluginService(ApiClient api) {
        this.api = api;
    }

    public List<Plugin> list(Node node) throws APIException, IOException {
        return  api.path(routes.PluginResource().list(), ListPluginResponse.class).node(node).execute().plugins;
    }
}
