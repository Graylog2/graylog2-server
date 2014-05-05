package org.graylog2.restclient.models.api.responses.streams;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class TestMatchResponse {
    public Boolean matches;
    public Map<String, Boolean> rules;
}
