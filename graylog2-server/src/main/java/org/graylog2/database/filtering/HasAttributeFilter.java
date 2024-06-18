package org.graylog2.database.filtering;

import java.util.List;

public interface HasAttributeFilter {
    List<AttributeFilter> attributes();
}
