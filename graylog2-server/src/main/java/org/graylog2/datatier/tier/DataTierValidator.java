package org.graylog2.datatier.tier;

import org.graylog2.indexer.IndexSetValidator;

import java.util.List;
import java.util.Optional;

public interface DataTierValidator {

    Optional<IndexSetValidator.Violation> validate(List<DataTier> dataTiers);
}
