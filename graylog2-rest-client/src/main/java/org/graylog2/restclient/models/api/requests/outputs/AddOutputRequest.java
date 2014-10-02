package org.graylog2.restclient.models.api.requests.outputs;

import org.graylog2.restclient.models.api.requests.ApiRequest;

import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AddOutputRequest extends ApiRequest {
    public Set<String> outputs;
}
