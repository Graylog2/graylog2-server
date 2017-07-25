package org.graylog.plugins.netflow.v9;

import com.google.common.collect.ImmutableMap;

public interface NetFlowV9BaseRecord {
    ImmutableMap<String, Object> fields();
}
