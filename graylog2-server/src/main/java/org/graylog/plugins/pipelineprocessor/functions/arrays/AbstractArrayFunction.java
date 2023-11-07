package org.graylog.plugins.pipelineprocessor.functions.arrays;

import com.google.common.collect.ImmutableList;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;

import java.util.Collection;
import java.util.List;

abstract public class AbstractArrayFunction<T> extends AbstractFunction<T> {
    @SuppressWarnings("rawtypes")
    static List toList(Object obj) {
        if (obj instanceof Collection) {
            return ImmutableList.copyOf((Collection) obj);
        } else {
            throw new IllegalArgumentException("Unsupported data type for parameter 'elements': " + obj.getClass().getCanonicalName());
        }
    }
}
