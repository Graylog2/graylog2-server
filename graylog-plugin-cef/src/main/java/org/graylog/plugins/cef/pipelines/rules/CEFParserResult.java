package org.graylog.plugins.cef.pipelines.rules;

import com.google.common.collect.ForwardingMap;

import java.util.Map;

public class CEFParserResult extends ForwardingMap<String, Object> {
    private final Map<String, Object> results;

    public CEFParserResult(Map<String, Object> fields) {
        this.results = fields;
    }

    @Override
    protected Map<String, Object> delegate() {
        return results;
    }

}
