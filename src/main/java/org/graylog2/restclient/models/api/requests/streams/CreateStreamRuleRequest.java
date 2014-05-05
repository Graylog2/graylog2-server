package org.graylog2.restclient.models.api.requests.streams;

import org.graylog2.restclient.models.api.requests.ApiRequest;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateStreamRuleRequest extends ApiRequest {
    public String field;
    public Integer type;
    public String value;
    public Boolean inverted;
}
