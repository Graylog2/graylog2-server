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
import React from 'react';
import * as Immutable from 'immutable';
import { renderHook } from 'wrappedTestingLibrary/hooks';

import {
  getLutParameters,
  getRestParameterValues,
  transformSearchFiltersToQuery,
  replaceParametersInQueryString,
  getStreams,
} from 'views/logic/valueactions/createEventDefinition/hooks/hookHelpers';
import useMappedData from 'views/logic/valueactions/createEventDefinition/hooks/useMappedData';
import {
  ltParam,
  ltParamJSON,
  mockedFilter,
  mockedSearchFilters,
  mockedView,
  parameterBindings,
  parameters,
  testAggregationWidget,
  valueParameter,
  valueParameterJSON,
  valuePath,
} from 'fixtures/createEventDefinitionFromValue';
import asMock from 'helpers/mocking/AsMock';

jest.mock('views/logic/valueactions/createEventDefinition/hooks/hookHelpers', () => ({
  ...jest.requireActual('views/logic/valueactions/createEventDefinition/hooks/hookHelpers'),
  // getAggregationHandler: jest.fn(),
  getLutParameters: jest.fn(),
  getRestParameterValues: jest.fn(),
  transformSearchFiltersToQuery: jest.fn(),
  replaceParametersInQueryString: jest.fn(),
  getStreams: jest.fn(),
  __esModule: true,
}));

const wrapper = ({ children }) => (
  <div>
    {children}
  </div>
);

describe('useMappedData', () => {
  it('runs all he;per functions', async () => {
    asMock(getStreams).mockReturnValue(['streamId']);
    // asMock(getAggregationHandler).mockReturnValue(aggregationMetricValueHandler);
    asMock(getLutParameters).mockReturnValue([ltParamJSON]);
    asMock(getRestParameterValues).mockReturnValue([valueParameterJSON]);
    asMock(transformSearchFiltersToQuery).mockReturnValue('(http_method:GET)');
    asMock(replaceParametersInQueryString).mockReturnValue('http_method:GET');

    const { result, waitFor } = renderHook(() => useMappedData({
      contexts: {
        widget: testAggregationWidget,
        view: mockedView,
        parameterBindings: parameterBindings,
        valuePath: valuePath,
        parameters: parameters,
        analysisDisabledFields: undefined,
        currentUser: undefined,
        message: undefined,
        isLocalNode: undefined,
      },
      field: 'action',
      value: 'show',
      queryId: 'query-id',
    }), { wrapper });
    await waitFor(() => !!result.current);

    await expect(getStreams).toHaveBeenCalledWith(mockedFilter);

    await expect(getLutParameters).toHaveBeenCalledWith(Immutable.Set([ltParam, valueParameter]));

    await expect(getRestParameterValues).toHaveBeenCalledWith({
      parameters,
      parameterBindings,
    });

    await expect(transformSearchFiltersToQuery).toHaveBeenCalledWith(mockedSearchFilters);

    await expect(replaceParametersInQueryString).toHaveBeenCalledWith({
      query: 'http_method:GET', restParameterValues: [valueParameterJSON],
    });
    // await expect(getAggregationHandler).toHaveBeenCalled();

    expect(result.current).toEqual({
      searchWithinMs: 300000,
      lutParameters: [ltParamJSON],
      searchFilterQuery: '(http_method:GET)',
      queryWithReplacedParams: 'http_method:GET',
      streams: ['streamId'],
      searchFromValue: 'action:show',
    });
  });
});
