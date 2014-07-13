package org.graylog2.streams;

import com.google.inject.ImplementedBy;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamOutput;
import org.graylog2.streams.outputs.CreateStreamOutputRequest;

import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@ImplementedBy(StreamOutputServiceImpl.class)
public interface StreamOutputService extends PersistedService {
    StreamOutput load(String streamOutputId) throws NotFoundException;
    Set<StreamOutput> loadAll();
    Set<StreamOutput> loadAllForStream(String streamId);
    Set<StreamOutput> loadAllForStream(Stream stream);
    StreamOutput create(StreamOutput request);
    StreamOutput create(final String streamId, final CreateStreamOutputRequest request);
}
