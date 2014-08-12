package org.graylog2.rest.resources.streams.rules.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@JsonAutoDetect
public class CreateStreamRuleRequest {
    public Integer type;
    public String value;
    public String field;
    public Boolean inverted;
}
