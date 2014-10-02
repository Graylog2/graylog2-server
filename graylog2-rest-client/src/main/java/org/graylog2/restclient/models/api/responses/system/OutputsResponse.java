package org.graylog2.restclient.models.api.responses.system;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputsResponse {
    public int total;
    public List<OutputSummaryResponse> outputs;
}
