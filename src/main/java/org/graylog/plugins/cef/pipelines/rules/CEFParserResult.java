package org.graylog.plugins.cef.pipelines.rules;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class CEFParserResult extends ForwardingMap<String, Object> {

    private final ImmutableMap<String, Object> results;

    public CEFParserResult(ImmutableMap<String, Object> fields) {
        this.results = fields;
    }

    public Map<String, Object> getResults() {
        return results;
    }

    @Override
    protected Map<String, Object> delegate() {
        return getResults();
    }

}
