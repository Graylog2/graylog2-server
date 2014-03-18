package org.graylog2.alerts.types;

import org.graylog2.Core;
import org.graylog2.alerts.AlertCondition;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class DummyAlertCondition extends AlertCondition {
    final String description = "Dummy alert to test notifications";

    public DummyAlertCondition(Core core, Stream stream, String id, Type type, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        super(core, stream, id, type, createdAt, creatorUserId, parameters);
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public CheckResult runCheck() {
        return new CheckResult(true, this, this.description, Tools.iso8601());
    }

    @Override
    public List<ResultMessage> getSearchHits() {
        return null;
    }
}
