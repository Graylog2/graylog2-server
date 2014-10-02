package org.graylog2.restclient.models.api.responses.streams;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleSummaryResponse {
    public String id;
    public String field;
    public String value;
    public int type;
    public Boolean inverted;

    public String stream_id;
}
