package org.graylog2.restclient.models.api.requests.streams;

import org.graylog2.restclient.models.api.requests.ApiRequest;
import play.data.validation.Constraints;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class TestMatchRequest extends ApiRequest {
    //@Constraints.Required
    public Map<String, Object> message;
}
