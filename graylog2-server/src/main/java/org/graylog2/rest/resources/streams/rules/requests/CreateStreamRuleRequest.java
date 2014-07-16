package org.graylog2.rest.resources.streams.rules.requests;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateStreamRuleRequest {
    public Integer type;
    public String value;
    public String field;
    public Boolean inverted;
}
