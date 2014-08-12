package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.graylog2.plugin.streams.Stream;

import java.util.Collection;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@JsonAutoDetect
public class StreamListResponse {
    public long total;
    public Collection<Stream> streams;
}
