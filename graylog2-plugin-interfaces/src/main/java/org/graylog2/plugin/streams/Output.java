package org.graylog2.plugin.streams;

import org.graylog2.plugin.database.Persisted;

import java.util.Date;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface Output extends Persisted {
    public String getTitle();
    public String getType();
    public Map<String, Object> getConfiguration();
    public Date getCreatedAt();
}
