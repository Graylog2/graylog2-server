package org.graylog2.rest.resources.streams.responses;

import org.graylog2.plugin.streams.Stream;

import java.util.Collection;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamListResponse {
    public long total;
    public Collection<Stream> streams;
}
