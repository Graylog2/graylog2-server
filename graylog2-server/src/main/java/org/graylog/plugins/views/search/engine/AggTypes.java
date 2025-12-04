package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;

import java.util.HashMap;
import java.util.Map;

public class AggTypes {
    final Map<PivotSpec, String> aggMap = new HashMap<>();

    public void record(PivotSpec pivotSpec, String name) {
        aggMap.put(pivotSpec, name);
    }

    public String getTypes(PivotSpec pivotSpec) {
        return aggMap.get(pivotSpec);
    }
}
