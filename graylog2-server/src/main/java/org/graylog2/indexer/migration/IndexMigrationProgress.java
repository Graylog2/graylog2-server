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
package org.graylog2.indexer.migration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record IndexMigrationProgress(long total, long created, long updated, long deleted, long versionConflicts,
                                     long noops) {

    private static final BigDecimal ONE_HUNDRED_PERCENT = new BigDecimal(100);

    public int progressPercent() {
        return progress().scaleByPowerOfTen(2).intValue();
    }

    /**
     * @return Value between 0 and 1, progress of the index migration
     */
    @JsonIgnore
    public BigDecimal progress() {
        if (total == 0) { // avoid division by zero
            return BigDecimal.ONE;
        }
        final BigDecimal value = BigDecimal.valueOf(created + updated + deleted + versionConflicts + noops);
        return value.divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }
}
