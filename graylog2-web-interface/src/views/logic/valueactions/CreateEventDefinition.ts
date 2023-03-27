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
import type { ActionHandlerArguments } from 'views/components/actions/ActionHandler';
import type Widget from 'views/logic/widgets/Widget';
import type { EventDefinitionURLConfig } from 'components/event-definitions/hooks/useEventDefinitionConfigFromUrl';
import { seriesToMetrics } from 'views/components/aggregationwizard/metric/MetricElement';
import type { FilterType, RelativeTimeRangeWithEnd } from 'views/logic/queries/Query';
import type { FiltersType } from 'views/types';

type AggregationHandler = (args: { widget?: Widget, field: string, value: string, valuePath?: Array<{ [name: string]: string}>, })=>{
  agg_field?: string,
  agg_function?: string,
  agg_value?: string,
  search?: string,
  valuePathQueryString?: string
}

const transformValuePathToQuery = (valuePath: Array<{ [name: string]: string}>) => {
  return valuePath.reduce((res, path, index) => {
    if (index === valuePath.length - 1) return res;

    const [field, value] = Object.entries(path)[0];

    return `${res}${res ? ' AND ' : ' '}${field}:${value}`;
  }, '');
};

const aggregationMetricValueHandler: AggregationHandler = ({ widget, value, field, valuePath }) => {
  const curSeries = widget.config.series.find((series) => series.function === field);
  const { field: agg_field, function: agg_function } = seriesToMetrics([curSeries])[0];
  const { rowPivots } = widget.config;

  const group_by = Array.from(rowPivots.reduce((res, cur) => {
    cur.fields.forEach((pivotField) => res.add(pivotField));

    return res;
  }, new Set([])));

  const valuePathQueryString = transformValuePathToQuery(valuePath);

  return ({
    agg_field,
    agg_function,
    agg_value: value,
    group_by,
    valuePathQueryString,
  });
};

const aggregationValueHandler: AggregationHandler = ({ value, field, valuePath }) => {
  const valuePathQueryString = transformValuePathToQuery(valuePath);

  return ({
    search: `${field}:${value}`,
    valuePathQueryString,
  });
};

const messagesValueHandler: AggregationHandler = ({ value, field }) => {
  return ({
    search: `${field}:${value}`,
  });
};

const logsValueHandler: AggregationHandler = ({ value, field }) => {
  return ({
    search: `${field}:${value}`,
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

const getLutParameters = (parameters) => parameters.reduce((res, cur) => {
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

const transformSearchFiltersToQuery = (filters: FiltersType) => {
  return filters.reduce((res, filter) => {
    let curRes = res;

    if (filter.queryString) {
      curRes = `${res}${res ? ' AND ' : ' '}(${filter.queryString})`;
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
  return filter.get('filters').reduce((res, curFilter) => {
    if (curFilter.get('type') === 'stream') {
      res.push(curFilter.get('id'));
    }

    return res;
  }, []);
};

const concatQuery = (queryParts: Array<string>) => {
  return queryParts.reduce((res, queryPart) => {
    let curRes = res;

    if (queryPart) {
      curRes = `${res}${res ? ' AND ' : ' '}(${queryPart})`;
    }

    return curRes;
  }, '');
};

const CreateEventDefinition = (args: ActionHandlerArguments) => {
  console.log(
    args,
  );

  const aggregationHandler = getAggregationHandler({ widget: args.contexts.widget, field: args.field });
  const curQuery = args.contexts.view.search.queries.find((query) => query.id === args.queryId);
  const { parameters, parameterBindings } = args.contexts;
  const search_within_ms = curQuery.timerange.type === 'relative' ? (curQuery.timerange as RelativeTimeRangeWithEnd).from * 1000 : undefined;
  const lutParameters = getLutParameters(parameters);
  const restParameterValues = getRestParameterValues({ parameters, parameterBindings });
  const searchFilterQuery = transformSearchFiltersToQuery(curQuery.filters);
  const queryWithReplacedParams = replaceParametersInQueryString({ query: curQuery.query.query_string, restParameterValues });
  const streams = getStreams(curQuery.filter);
  const { search: searchFromAggregation, valuePathQueryString, ...aggregationVales } = aggregationHandler({ valuePath: args.contexts.valuePath, widget: args.contexts.widget, value: args.value, field: args.field });

  console.log({
    queryWithReplacedParams,
    searchFilterQuery,
    searchFromAggregation,
    valuePathQueryString,
  });

  const eventDefinitionUrlConfig: EventDefinitionURLConfig = ({
    query: concatQuery([queryWithReplacedParams, searchFilterQuery, searchFromAggregation, valuePathQueryString]),
    loc_query_parameters: lutParameters,
    search_within_ms,
    type: 'aggregation-v1',
    streams,
    ...aggregationVales,
  });

  console.log({ filter: curQuery.filter.get('filters') });

  console.log({ eventDefinitionUrlConfig });

  return Promise.resolve();
};

export default CreateEventDefinition;
