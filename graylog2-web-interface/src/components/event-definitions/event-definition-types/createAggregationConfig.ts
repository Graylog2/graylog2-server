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

import getSeriesId from './getSeriesId';

export type AggregationCondition = {
  function: string;
  field?: string | null;
  operator: string;
  threshold: number | string;
  group_by?: Array<string>;
};

// Series and condition as the condition form builds them for a single `function(field) operator threshold`.
const createAggregationConfig = ({
  function: type,
  field,
  operator,
  threshold,
  group_by = [],
}: AggregationCondition) => {
  const seriesId = getSeriesId({ type, field });

  return {
    group_by,
    series: [{ id: seriesId, type, field: field || null }],
    conditions: {
      expression: {
        expr: operator,
        left: { expr: 'number-ref', ref: seriesId },
        right: { expr: 'number', value: threshold },
      },
    },
  };
};

export default createAggregationConfig;
