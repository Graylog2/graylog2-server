package org.graylog2.restclient.models;

import org.graylog2.restclient.models.api.responses.AvailableOutputSummary;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputTypesResponse {
    public Map<String, AvailableOutputSummary> types;
}
