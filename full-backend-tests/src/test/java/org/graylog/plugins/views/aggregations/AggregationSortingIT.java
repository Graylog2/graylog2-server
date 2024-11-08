package org.graylog.plugins.views.aggregations;

import com.github.rholder.retry.RetryException;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SortSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.inputs.PortBoundGelfInputApi;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.containermatrix.SearchServer.ES7;
import static org.graylog.testing.containermatrix.SearchServer.OS1;
import static org.graylog.testing.containermatrix.SearchServer.OS2_LATEST;

@ContainerMatrixTestsConfiguration(searchVersions = {ES7, OS1, OS2_LATEST})
public class AggregationSortingIT {
    private final GraylogApis api;
    private PortBoundGelfInputApi gelfInput;

    public AggregationSortingIT(GraylogApis api) {
        this.api = api;
    }

    @BeforeAll
    void setUp() {
        this.gelfInput = api.gelf().createGelfHttpInput();
    }

    @ContainerMatrixTest
    void sortingOnNumericPivotFieldSortsNumerically() throws ExecutionException, RetryException {
        try (final var env = createEnvironment()) {
            for (final var value : Set.of(9, 8, 4, 25, 2, 15, 1)) {
                env.ingestMessage(Map.of(
                        "non_numeric_field", "foo",
                        "numeric_field", value,
                        "short_message", "sorting on numeric pivot test " + value
                ));
            }

            api.search().waitForMessage("sorting on numeric pivot test 1");
            final var result = env.executePivot(Pivot.builder()
                    .rowGroups(Values.builder()
                            .fields(List.of("non_numeric_field", "numeric_field")).limit(10).build())
                    .sort(PivotSort.create("numeric_field", SortSpec.Direction.Descending))
                    .build()).log().body(true);
            assertThat(result).isNotNull();
        }
    }

    private GraylogApis.SearchEnvironment createEnvironment() throws ExecutionException, RetryException {
        return api.createEnvironment(gelfInput);
    }
}
