package org.graylog.plugins.views.search.searchtypes.pivot.series;

import java.util.Optional;

public interface HasOptionalField {
    Optional<String> field();
}
