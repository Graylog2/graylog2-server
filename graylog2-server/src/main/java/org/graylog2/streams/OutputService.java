package org.graylog2.streams;

import com.google.inject.ImplementedBy;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.outputs.CreateOutputRequest;

import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@ImplementedBy(OutputServiceImpl.class)
public interface OutputService extends PersistedService {
    Output load(String streamOutputId) throws NotFoundException;
    Set<Output> loadAll();
    Set<Output> loadForStream(Stream stream);
    Output create(Output request) throws ValidationException;
    Output create(CreateOutputRequest request) throws ValidationException;
}
