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
import AggregationWidgetConfig from './AggregationWidgetConfig';
import Series from './Series';
import SortConfig from './SortConfig';

describe('AggregationWidgetConfig', () => {
  it('do not enable rollups if no column pivots are present (was default in the past)', () => {
    const config = AggregationWidgetConfig.builder()
      .columnPivots([])
      .rollup(false)
      .build();

    expect(config.rollup).toEqual(false);
  });

  it('filters sorts referencing nonpresent metrics', () => {
    const config = AggregationWidgetConfig.builder()
      .series([Series.forFunction('count()')])
      .sort([SortConfig.fromSeries(Series.forFunction('avg(field1)'))])
      .build();

    expect(config.sort).toEqual([]);
  });
});
