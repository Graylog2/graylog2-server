package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;

import java.util.HashMap;
import java.util.Map;

public class PivotAggsContext {
    final Map<PivotSpec, String> aggMap = new HashMap<>();

    public void record(final PivotSpec pivotSpec, final String name) {
        aggMap.put(pivotSpec, name);
    }

    public String getTypes(final PivotSpec pivotSpec) {
        return aggMap.get(pivotSpec);
    }
}
