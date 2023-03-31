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
import { useMemo } from 'react';
import pickBy from 'lodash/pickBy';
import isArray from 'lodash/isArray';
import isNumber from 'lodash/isNumber';

import type { AggregationHandler, MappedData } from 'views/logic/valueactions/createEventDefinition/types';
import { seriesToMetrics } from 'views/components/aggregationwizard/metric/MetricElement';
import type Widget from 'views/logic/widgets/Widget';
import type { ActionContexts, FiltersType } from 'views/types';
import type { FilterType, RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import type { ActionComponentProps } from 'views/components/actions/ActionHandler';
import type Parameter from 'views/logic/parameters/Parameter';

const transformValuePathToQuery = (valuePath: Array<{ [name: string]: string}>) => {
  return valuePath.reduce((res, path) => {
    const key = Object.keys(path)[0];
    if (key === '_exists_') return res;

    const [field, value] = Object.entries(path)[0];

    return `${res}${res ? ' AND ' : ''}${field}:${value}`;
  }, '');
};

const getFlattenPivots = (pivots): Set<string> => {
  return pivots.reduce((res, cur) => {
    cur.fields.forEach((pivotField) => res.add(pivotField));

    return res;
  }, new Set([]));
};

const filtratePathsByPivot = ({ flattenPivots, valuePath }: {flattenPivots: Set<string>, valuePath: Array<{ [name: string]: string }>}): Array<{[name:string]: string}> => {
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

const aggregationMetricValueHandler: AggregationHandler = ({ widget, value, field, valuePath }) => {
  const curSeries = widget.config.series.find((series) => series.function === field);
  const { field: agg_field, function: agg_function } = seriesToMetrics([curSeries])[0];
  const { rowPivots, columnPivots } = widget.config;
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

const aggregationValueHandler: AggregationHandler = ({ widget, value, field, valuePath }) => {
  const { rowPivots } = widget.config;
  const flattenRowPivots = getFlattenPivots(rowPivots);
  const rowPaths = filtratePathsByPivot({ flattenPivots: flattenRowPivots, valuePath });
  const rowValuePath = transformValuePathToQuery(rowPaths);

  return ({
    searchFromValue: `${field}:${value}`,
    rowValuePath,
  });
};

const messagesValueHandler: AggregationHandler = ({ value, field }) => {
  return ({
    searchFromValue: `${field}:${value}`,
  });
};

const logsValueHandler: AggregationHandler = ({ value, field }) => {
  return ({
    searchFromValue: `${field}:${value}`,
  });
};

const getAggregationHandler = ({ widget, field }: { widget: Widget, field: string }): AggregationHandler => {
  if (widget.type === 'AGGREGATION') {
    const isMetrics = !!widget.config.series.find((series) => series.function === field);

    return isMetrics ? aggregationMetricValueHandler : aggregationValueHandler;
  }

  if (widget.type === 'logs') return logsValueHandler;
  if (widget.type === 'messages') return messagesValueHandler;

  throw new Error('This widget type has incorrect type or no handler');
};

const getLutParameters = (parameters: Immutable.Set<Parameter>) => parameters.reduce((res, cur) => {
  if (cur.type === 'lut-parameter-v1') {
    const paramJSON = cur.toJSON();
    res.push(paramJSON);
  }

  return res;
}, []);

const getRestParameterValues = ({ parameters, parameterBindings }) => parameters.reduce((res, cur) => {
  if (cur.type !== 'lut-parameter-v1') {
    const paramJSON = cur.toJSON();
    const { name } = paramJSON;
    const binding = parameterBindings.get(name);
    res[name] = binding.value ?? paramJSON.defaultValue;
  }

  return res;
}, {});

const transformSearchFiltersToQuery = (filters: FiltersType = Immutable.List([])) => {
  return filters.reduce((res, filter) => {
    let curRes = res;

    if (filter.queryString && !filter.disabled) {
      curRes = `${res}${res ? ' AND ' : ''}(${filter.queryString})`;
    }

    return curRes;
  }, '');
};

const replaceParametersInQueryString = ({ query, restParameterValues }: {
  query: string,
  restParameterValues: { [name: string]: string | number }
}) => {
  let curQuery = query;

  Object.entries(restParameterValues).forEach(([parameterName, parameterValue]) => {
    curQuery = curQuery.replace(`$${parameterName}$`, `${parameterValue}`);
  });

  return curQuery;
};

const getStreams = (filter: FilterType): Array<string> => {
  if (!filter) return [];

  return filter.get('filters').reduce((res, curFilter) => {
    if (curFilter.get('type') === 'stream') {
      res.push(curFilter.get('id'));
    }

    return res;
  }, []);
};

type HookProps = Pick<ActionComponentProps, 'field' | 'queryId' | 'value'> & { contexts: ActionContexts }
const useMappedData = ({ contexts, field, queryId, value }: HookProps) => useMemo<MappedData>(() => {
  const aggregationHandler = getAggregationHandler({ widget: contexts.widget, field: field });
  const curQuery = contexts.view.search.queries.find((query) => query.id === queryId);
  const { parameters, parameterBindings } = contexts;
  const searchWithinMs = curQuery.timerange.type === 'relative' ? (curQuery.timerange as RelativeTimeRangeWithEnd).from * 1000 : undefined;
  const lutParameters = getLutParameters(parameters);
  const restParameterValues = getRestParameterValues({ parameters, parameterBindings });
  const searchFilterQuery = transformSearchFiltersToQuery(curQuery.filters);
  const queryWithReplacedParams = replaceParametersInQueryString({ query: curQuery.query.query_string, restParameterValues });
  const streams = getStreams(curQuery.filter);
  const { ...aggregationVales } = aggregationHandler({ valuePath: contexts.valuePath, widget: contexts.widget, value: value, field: field });
  const data: MappedData = {
    searchWithinMs,
    lutParameters,
    searchFilterQuery,
    queryWithReplacedParams,
    streams,
    ...aggregationVales,
  };

  return pickBy(data, (v) => {
    if (isArray(v)) {
      return !!v.length;
    }

    if (isNumber(v)) {
      return true;
    }

    return !!v?.trim();
  });
}, [contexts, field, queryId, value]);

export default useMappedData;
