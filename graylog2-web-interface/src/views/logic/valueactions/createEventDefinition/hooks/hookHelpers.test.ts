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
import * as Immutable from 'immutable';

import {
  filtratePathsByPivot,
  getFlattenPivots,
  transformValuePathToQuery,
  aggregationMetricValueHandler,
  aggregationValueHandler,
  messagesValueHandler,
  logsValueHandler,
  getAggregationHandler,
  getLutParameters,
  getRestParameterValues,
  transformSearchFiltersToQuery,
  replaceParametersInQueryString,
  getStreams,
} from 'views/logic/valueactions/createEventDefinition/hooks/hookHelpers';
import {
  disabledSearchFilter,
  firstSimpleSearchFilter,
  ltParamJSON,
  messageTable, negationSearchFilter, parameters,
  pivots, secondSimpleSearchFilter,
  testAggregationWidget,
  testWidgetMetricValuePath,
  testWidgetValueValuePath,
  valuePath,
} from 'fixtures/createEventDefinitionFromValue';
import ParameterBinding from 'views/logic/parameters/ParameterBinding';

describe('useMappedData helper function', () => {
  describe('transformValuePathToQuery', () => {
    it('return query string', () => {
      const result = transformValuePathToQuery(valuePath);

      expect(result).toEqual('http_method:GET AND action:index');
    });

    it('ignore _exists_ key', () => {
      const result = transformValuePathToQuery([...valuePath, { _exists_: 'count(action)' }]);

      expect(result).toEqual('http_method:GET AND action:index');
    });
  });

  it('getFlattenPivots return fields without duplication', () => {
    const result = getFlattenPivots(pivots);
    const expected = new Set(['http_method', 'index', 'resources']);

    expect(result).toEqual(expected);
  });

  describe('filtratePathsByPivot', () => {
    it('return pivots for path', () => {
      const result = filtratePathsByPivot(
        {
          flattenPivots: new Set(['http_method']),
          valuePath,
        });
      const expected = [{ http_method: 'GET' }];

      expect(result).toEqual(expected);
    });

    it('return empty array when no matches', () => {
      const resultEmptyPath = filtratePathsByPivot(
        {
          flattenPivots: new Set(['http_method']),
          valuePath: [],
        });

      expect(resultEmptyPath).toEqual([]);

      const resultNoMatches = filtratePathsByPivot(
        {
          flattenPivots: new Set(['http_method', 'resources', 'action']),
          valuePath: [{ controller: 'Controller' }],
        });

      expect(resultNoMatches).toEqual([]);
    });
  });

  it('aggregationMetricValueHandler return correct data', async () => {
    const result = aggregationMetricValueHandler({
      widget: testAggregationWidget,
      valuePath: testWidgetMetricValuePath,
      field: 'count(field5)',
      value: 999,
    });

    expect(result).toEqual({
      aggField: 'field5',
      aggFunction: 'count',
      aggValue: 999,
      rowGroupBy: ['field3', 'field4'],
      columnGroupBy: ['field1', 'field2'],
      rowValuePath: 'field3:value3 AND field4:value4',
      columnValuePath: 'field1:value1',
    });
  });

  it('aggregationValueHandler return correct data', async () => {
    const result = aggregationValueHandler({
      widget: testAggregationWidget,
      valuePath: testWidgetValueValuePath,
      field: 'field4',
      value: 'value4',
    });

    expect(result).toEqual({
      searchFromValue: 'field4:value4',
      rowValuePath: 'field3:value3 AND field4:value4',
    });
  });

  it('messagesValueHandler return correct data', async () => {
    const result = messagesValueHandler({
      field: 'field4',
      value: 'value4',
    });

    expect(result).toEqual({
      searchFromValue: 'field4:value4',
    });
  });

  it('logsValueHandler return correct data', async () => {
    const result = logsValueHandler({
      field: 'field4',
      value: 'value4',
    });

    expect(result).toEqual({
      searchFromValue: 'field4:value4',
    });
  });

  describe('getAggregationHandler', () => {
    it('return aggregation metric handler', () => {
      const result = getAggregationHandler({ widget: testAggregationWidget, field: 'count(field5)' });

      expect(result).toEqual(aggregationMetricValueHandler);
    });

    it('return aggregation value handler', () => {
      const result = getAggregationHandler({ widget: testAggregationWidget, field: 'field4' });

      expect(result).toEqual(aggregationValueHandler);
    });

    it('return message handler', () => {
      const result = getAggregationHandler({ widget: messageTable, field: 'field4' });

      expect(result).toEqual(messagesValueHandler);
    });
  });

  it('getLutParameters return only lookup table parameters in JSON format', async () => {
    const result = getLutParameters(parameters);

    expect(result).toEqual([ltParamJSON]);
  });

  describe('getRestParameterValues return non lookup table parameters', () => {
    it('with default value if binding doesnt exist', async () => {
      const result = getRestParameterValues({ parameters });

      expect(result).toEqual({ newParameter3: 'GET' });
    });

    it('with binding value', async () => {
      const result = getRestParameterValues({ parameters, parameterBindings: Immutable.Map({ newParameter3: new ParameterBinding('value', 'POST') }) });

      expect(result).toEqual({ newParameter3: 'POST' });
    });
  });

  describe('transformSearchFiltersToQuery return query string', () => {
    it('for all filters', async () => {
      const result = transformSearchFiltersToQuery(Immutable.List([firstSimpleSearchFilter, secondSimpleSearchFilter]));

      expect(result).toEqual('(http_method: GET or http_method: POST) AND (action: show)');
    });

    it('without disabled filters', async () => {
      const result = transformSearchFiltersToQuery(Immutable.List([firstSimpleSearchFilter, disabledSearchFilter, secondSimpleSearchFilter]));

      expect(result).toEqual('(http_method: GET or http_method: POST) AND (action: show)');
    });

    it('with NOT operator for negation filters', async () => {
      const result = transformSearchFiltersToQuery(Immutable.List([firstSimpleSearchFilter, negationSearchFilter, secondSimpleSearchFilter]));

      expect(result).toEqual('(http_method: GET or http_method: POST) AND NOT(action: login) AND (action: show)');
    });
  });

  it('replaceParametersInQueryString return queryString with replaced parameters', async () => {
    const result = replaceParametersInQueryString({
      query: 'http_Method:$newParameter3$ or http_Method:$newParameter2$',
      restParameterValues: { newParameter3: 'POST', newParameter2: 'GET' },
    });

    expect(result).toEqual('http_Method:POST or http_Method:GET');
  });

  it('getStreams return only  streams ids', async () => {
    const result = getStreams(Immutable.Map([
      ['filters', Immutable.Set([
        Immutable.Map([['type', 'stream'], ['id', 'stream-id']]),
        Immutable.Map([['type', 'some-type'], ['id', 'non-stream-id']]),
      ])],
    ]));

    expect(result).toEqual(['stream-id']);
  });
});
