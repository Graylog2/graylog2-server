package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

import com.google.common.annotations.VisibleForTesting;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValuesBucketOrdering {
    private static boolean isGroupingSort(SortSpec sort) {
        return "pivot".equals(sort.type());
    }

    private static boolean hasGroupingSort(List<SortSpec> sorts) {
        return sorts.stream().anyMatch(ValuesBucketOrdering::isGroupingSort);
    }

    private static boolean needsReordering(List<? extends BucketSpec> bucketSpec, List<SortSpec> sorts) {
        return bucketSpec.size() >= 2 && !sorts.isEmpty() && hasGroupingSort(sorts);
    }

    @VisibleForTesting
    public static <T extends BucketSpec> List<T> orderBuckets(List<T> bucketSpec, List<SortSpec> sorts) {
        if (!needsReordering(bucketSpec, sorts)) {
            return bucketSpec;
        }

        final List<String> sortFields = sorts.stream()
                .filter(ValuesBucketOrdering::isGroupingSort)
                .map(SortSpec::field)
                .collect(Collectors.toList());

        return bucketSpec.stream()
                .sorted((s1, s2) -> {
                    int s1Index = sortFields.indexOf(s1.field());
                    int s2Index = sortFields.indexOf(s2.field());
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
                })
                .collect(Collectors.toList());
    }

    public static Function<List<String>, List<String>> reorderKeysFunction(List<BucketSpec> bucketSpecs, List<SortSpec> sorts) {
        if (!needsReordering(bucketSpecs, sorts)) {
            return Function.identity();
        }

        final List<BucketSpec> orderedBuckets = orderBuckets(bucketSpecs, sorts);
        final Map<Integer, Integer> mapping = IntStream.range(0, bucketSpecs.size())
                .boxed()
                .collect(Collectors.toMap(Function.identity(), i -> orderedBuckets.indexOf(bucketSpecs.get(i))));

        return (keys) -> IntStream.range(0, bucketSpecs.size())
                .boxed()
                .map(i -> keys.get(mapping.get(i)))
                .collect(Collectors.toList());
    }
}
