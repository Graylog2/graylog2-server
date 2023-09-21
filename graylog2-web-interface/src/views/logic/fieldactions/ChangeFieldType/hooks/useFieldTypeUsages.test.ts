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

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import suppressConsole from 'helpers/suppressConsole';
import { qualifyUrl } from 'util/URLUtils';
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
  total: 1,
  sort: 'index_set_title',
  order: 'desc',
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
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

const renderUseFieldTypeUsagesHook = () => renderHook(() => useFieldTypeUsages({ streams: ['001'], field: 'field' }, { query: '', page: 1, pageSize: 10, sort: { attributeId: '', direction: 'asc' } }));

describe('useFieldTypeUsages custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve(mockFieldTypeUsages));
    const { result, waitFor } = renderUseFieldTypeUsagesHook();

    await waitFor(() => !result.current.isFirsLoaded);
    await waitFor(() => result.current.isFirsLoaded);

    expect(fetch).toHaveBeenCalledWith('POST', qualifyUrl('/system/indices/index_sets/types?page=1&per_page=10&sort=&order=asc'), {
      field: 'field',
      streams: ['001'],
    });

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

    const { result, waitFor } = renderUseFieldTypeUsagesHook();

    await suppressConsole(async () => {
      await waitFor(() => !result.current.isFirsLoaded);
      await waitFor(() => result.current.isFirsLoaded);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading field types failed with status: Error: Error',
      'Could not load field types');
  });
});
