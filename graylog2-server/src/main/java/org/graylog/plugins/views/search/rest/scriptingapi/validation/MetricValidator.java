/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.rest.scriptingapi.validation;

import org.apache.commons.lang.StringUtils;
import org.graylog.plugins.views.search.rest.SeriesDescription;
import org.graylog.plugins.views.search.rest.scriptingapi.request.Metric;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;

import javax.inject.Inject;
import javax.validation.ValidationException;
import java.util.Collection;
import java.util.Map;

public class MetricValidator {

    private final Collection<String> availableMetricTypes;

    @Inject
    public MetricValidator(final Map<String, SeriesDescription> availableFunctions) {
        this.availableMetricTypes = availableFunctions.keySet();
    }

    public void validate(final Metric metric) {
        if (metric == null) {
            throw new ValidationException("Metric cannot be null");
        }
        if (!isValidFunction(metric.functionName())) {
            throw new ValidationException("Unrecognized metric : " + metric.functionName() + ", valid metrics : " + availableMetricTypes);
        }
        if (!hasFieldIfFunctionNeedsIt(metric)) {
            throw new ValidationException(metric.functionName() + " metric requires field name to be provided after a colon, i.e. " + metric.functionName() + ":http_status_code");
        }
    }

    private boolean hasFieldIfFunctionNeedsIt(final Metric metric) {
        return Count.NAME.equals(metric.functionName()) || !StringUtils.isBlank(metric.fieldName());
    }

    private boolean isValidFunction(final String functionName) {
        return StringUtils.isNotBlank(functionName) && availableMetricTypes.contains(functionName);
    }
}
