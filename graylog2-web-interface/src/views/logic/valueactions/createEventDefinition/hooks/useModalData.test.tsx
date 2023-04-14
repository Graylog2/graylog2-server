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
import { renderHook } from 'wrappedTestingLibrary/hooks';

import MockStore from 'helpers/mocking/StoreMock';
import {
  ltParamJSON, modalDataResult,
} from 'fixtures/createEventDefinitionFromValue';
import useModalData from 'views/logic/valueactions/createEventDefinition/hooks/useModalData';

jest.mock('views/stores/StreamsStore', () => ({
  StreamsStore: MockStore(['getInitialState', () => ({
    streams: [
      { title: 'streamId-1-title', id: 'streamId-1' },
      { title: 'streamId-2-title', id: 'streamId-2' },
    ],
  })]),
}));

const wrapper = ({ children }) => (
  <div>
    {children}
  </div>
);

describe('useModalData', () => {
  it('return correct data', async () => {
    const { result, waitFor } = renderHook(() => useModalData({
      searchWithinMs: 300000,
      searchFilterQuery: '(http_method:GET)',
      queryWithReplacedParams: 'http_method:GET',
      searchFromValue: 'action:show',
      aggField: 'action',
      aggFunction: 'count',
      aggValue: 400,
      columnGroupBy: ['action', 'http_method'],
      rowGroupBy: ['action'],
      streams: ['streamId-1', 'streamId-2'],
      lutParameters: [ltParamJSON],
    }), { wrapper });
    await waitFor(() => !!result.current);

    expect(result.current).toEqual(modalDataResult);
  });
});
