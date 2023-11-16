package org.graylog2.datatier;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetValidator;

import javax.validation.constraints.NotNull;
import java.util.Optional;

public interface DataTierOrchestrator{

    void rotate(IndexSet indexSet);

    void retain(IndexSet indexSet);

    Optional<IndexSetValidator.Violation> validate(@NotNull DataTiersConfig config);

}
