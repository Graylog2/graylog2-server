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

import Immutable from 'immutable';

import type { FilterType } from 'views/logic/queries/Query';
import type { AggregationHandler } from 'views/logic/valueactions/createEventDefinition/types';
import { seriesToMetrics } from 'views/components/aggregationwizard/metric/MetricElement';
import type Widget from 'views/logic/widgets/Widget';
import type Parameter from 'views/logic/parameters/Parameter';
import type ValueParameter from 'views/logic/parameters/ValueParameter';
import type LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import type ParameterBinding from 'views/logic/parameters/ParameterBinding';
import type { FiltersType } from 'views/types';
import type AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import { concatQueryStrings, escape } from 'views/logic/queries/QueryHelper';

export const getStreams = (filter: FilterType): Array<string> => {
  if (!filter) return [];

  return filter.get('filters')
    .filter((curFilter) => curFilter.get('type') === 'stream')
    .map((curFilter) => curFilter.get('id'))
    .toArray();
};

export const transformValuePathToQuery = (valuePath: Array<{ [name: string]: string}>) => concatQueryStrings(valuePath.filter((path) => {
  const key = Object.keys(path)[0];

  return key !== '_exists_';
}).map((path) => {
  const [field, value] = Object.entries(path)[0];

  return `${field}:${escape(value)}`;
}), { withBrackets: false });

export const getFlattenPivots = (pivots: Array<Pivot>): Set<string> => new Set(pivots.flatMap(({ fields }) => fields));

export const filtratePathsByPivot = ({ flattenPivots, valuePath }: {flattenPivots: Set<string>, valuePath: Array<{ [name: string]: string }>}): Array<{[name:string]: string}> => {
  if (!valuePath) return [];
  const map = valuePath.reduce((res, cur) => {
    const key = Object.keys(cur)[0];

    if (!res.has(key) && flattenPivots.has(key)) {
      res.set(key, cur);
    }

    return res;
  }, new Map([])) as Map<string, {[name:string]: string}>;

  return Array.from(map.values());
};

export const aggregationMetricValueHandler: AggregationHandler = ({ widget, value, field, valuePath }) => {
  const curSeries = widget.config.series.find((series) => series.function === field);
  const { field: agg_field, function: agg_function } = seriesToMetrics([curSeries])[0];
  const { rowPivots, columnPivots } = widget.config as AggregationWidgetConfig;
  const flattenRowPivots = getFlattenPivots(rowPivots);
  const flattenColumnPivots = getFlattenPivots(columnPivots);
  const rowPaths = filtratePathsByPivot({ flattenPivots: flattenRowPivots, valuePath });
  const columnPaths = filtratePathsByPivot({ flattenPivots: flattenColumnPivots, valuePath });
  const rowValuePath = transformValuePathToQuery(rowPaths);
  const columnValuePath = transformValuePathToQuery(columnPaths);

  return ({
    aggField: agg_field,
    aggFunction: agg_function,
    aggValue: value,
    rowGroupBy: Array.from(flattenRowPivots),
    columnGroupBy: Array.from(flattenColumnPivots),
    rowValuePath,
    columnValuePath,
  });
};

export const aggregationValueHandler: AggregationHandler = ({ widget, value, field, valuePath }) => {
  const { rowPivots } = widget.config;
  const flattenRowPivots = getFlattenPivots(rowPivots);
  const rowPaths = filtratePathsByPivot({ flattenPivots: flattenRowPivots, valuePath });
  const rowValuePath = transformValuePathToQuery(rowPaths);

  return ({
    searchFromValue: `${field}:${escape(value)}`,
    rowValuePath,
  });
};

export const messagesValueHandler: AggregationHandler = ({ value, field }) => ({
  searchFromValue: `${field}:${escape(value)}`,
});

export const logsValueHandler: AggregationHandler = ({ value, field }) => ({
  searchFromValue: `${field}:${escape(value)}`,
});

export const getAggregationHandler = ({ widget, field }: { widget: Widget, field: string }): AggregationHandler => {
  if (widget.type === 'AGGREGATION') {
    const isMetrics = !!widget.config.series.find((series) => series.function === field);

    return isMetrics ? aggregationMetricValueHandler : aggregationValueHandler;
  }

  if (widget.type === 'logs') return logsValueHandler;
  if (widget.type === 'messages') return messagesValueHandler;

  throw new Error('This widget type has incorrect type or no handler');
};

export const getLutParameters = (parameters: Immutable.Set<Parameter>) => parameters.reduce((res, cur) => {
  if (cur.type === 'lut-parameter-v1') {
    const paramJSON = cur.toJSON();
    res.push(paramJSON);
  }

  return res;
}, []);

export const getRestParameterValues = (
  { parameters, parameterBindings }:
    { parameters: Immutable.Set<ValueParameter | LookupTableParameter>, parameterBindings?: Immutable.Map<string, ParameterBinding>},
) => parameters.reduce((res, cur) => {
  if (cur.type !== 'lut-parameter-v1') {
    const paramJSON = cur.toJSON();
    const { name } = paramJSON;
    const bindingValue = parameterBindings?.get(name)?.value;
    res[name] = bindingValue ?? paramJSON?.default_value;
  }

  return res;
}, {});

export const transformSearchFiltersToQuery = (filters: FiltersType = Immutable.List([])) => concatQueryStrings(filters
  .filter((filter) => (filter.queryString && !filter.disabled))
  .map((filter) => `${filter.negation ? 'NOT' : ''}(${filter.queryString})`).toArray(), { withBrackets: false });

export const replaceParametersInQueryString = ({ query, restParameterValues }: {
  query: string,
  restParameterValues: { [name: string]: string | number }
}) => {
  let curQuery = query;

  Object.entries(restParameterValues).forEach(([parameterName, parameterValue]) => {
    curQuery = curQuery.replace(`$${parameterName}$`, `${parameterValue}`);
  });

  return curQuery;
};
