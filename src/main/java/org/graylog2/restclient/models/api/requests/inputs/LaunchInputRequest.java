package org.graylog2.restclient.models.api.requests.inputs;

import play.data.validation.Constraints;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class LaunchInputRequest {
    @Constraints.Required
    public String title;
    @Constraints.Required
    public String type;
    public Boolean global = false;

    public String node;

    @Constraints.Required
    public Map<String, Object> configuration;
}
