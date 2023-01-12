package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import java.util.Comparator;
import java.util.List;

public class FieldsSortingComparator implements Comparator<String> {
    private final List<String> sortFields;

    public FieldsSortingComparator(List<String> sortFields) {
        this.sortFields = sortFields;
    }

    @Override
    public int compare(String s1, String s2) {
        if (sortFields.isEmpty()) {
            return 0;
        }
        int s1Index = sortFields.indexOf(s1);
        int s2Index = sortFields.indexOf(s2);
        if (s1Index == s2Index) {
            return 0;
        }
        if (s1Index == -1) {
            return 1;
        }
        if (s2Index == -1) {
            return -1;
        }
        return Integer.compare(s1Index, s2Index);
    }
}
