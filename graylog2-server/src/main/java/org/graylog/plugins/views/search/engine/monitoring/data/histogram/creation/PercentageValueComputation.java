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
package org.graylog.plugins.views.search.engine.monitoring.data.histogram.creation;

import java.util.Collection;

public class PercentageValueComputation<T> implements ValueComputation<T, Long> {

    @Override
    public Long computeValue(final Collection<T> elementsInBin, final int totalElements) {
        if (totalElements > 0) {
            return (long) (100 * ((float) elementsInBin.size() / totalElements));
        } else {
            return 0L;
        }
    }
}
