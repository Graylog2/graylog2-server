package org.graylog2.plugin.streams;

import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.outputs.MessageOutput;

import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface StreamOutput extends Persisted {
    public String getTitle();
    public String getType();
    public Set<String> getStreams();
    public Map<String, Object> getConfiguration();

    public void addStream(Stream stream);
}
