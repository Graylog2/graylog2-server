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

import { renderHook } from 'wrappedTestingLibrary/hooks';

import { SystemIndexSetsTypes } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import suppressConsole from 'helpers/suppressConsole';
import useFieldTypeUsages from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypeUsages';

const mockFieldTypeUsages = {
  attributes: [],
  query: '',
  pagination: {
    total: 1,
    count: 1,
    page: 1,
    per_page: 10,
  },
  defaults: {
    sort: {
      id: 'index_set_id',
      direction: 'ASC',
    } as { id: string, direction: 'ASC' | 'DESC'},
  },
  total: 1,
  sort: 'index_set_title',
  order: 'desc' as 'asc' | 'desc',
  elements: [
    {
      index_set_id: '0001',
      index_set_title: 'Index set title',
      stream_titles: [
        'Stream title',
      ],
      types: [
        'string',
      ],
    },
  ],
};

const expectedState = {
  attributes: [],
  list: [
    {
      id: '0001',
      indexSetTitle: 'Index set title',
      streamTitles: [
        'Stream title',
      ],
      types: [
        'string',
      ],
    },
  ],
  pagination: {
    total: 1,
  },
};
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

jest.mock('@graylog/server-api', () => ({
  SystemIndexSetsTypes: {
    fieldTypeSummaries: jest.fn(() => Promise.resolve()),
  },
}));

const renderUseFieldTypeUsagesHook = () => renderHook(() => useFieldTypeUsages({ streams: ['001'], field: 'field' }, { page: 1, pageSize: 10, sort: { attributeId: 'index_set_title', direction: 'asc' } }));

describe('useFieldTypeUsages custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(SystemIndexSetsTypes.fieldTypeSummaries).mockImplementation(() => Promise.resolve(mockFieldTypeUsages));
    const { result, waitFor } = renderUseFieldTypeUsagesHook();

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(SystemIndexSetsTypes.fieldTypeSummaries).toHaveBeenCalledWith({
      field: 'field',
      streams: ['001'],
    }, 'index_set_title', 1, 10, 'asc');

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(SystemIndexSetsTypes.fieldTypeSummaries).mockImplementation(() => Promise.reject(new Error('Error')));

    const { result, waitFor } = renderUseFieldTypeUsagesHook();

    await suppressConsole(async () => {
      await waitFor(() => result.current.isLoading);
      await waitFor(() => !result.current.isLoading);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading field types failed with status: Error: Error',
      'Could not load field types');
  });
});
