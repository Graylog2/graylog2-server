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

import { renderHook, waitFor } from 'wrappedTestingLibrary/hooks';

import { SystemIndexSetsTypes } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import suppressConsole from 'helpers/suppressConsole';
import useIndexSetFieldType from 'components/indices/IndexSetFieldTypes/hooks/useIndexSetFieldType';
import { defaultField } from 'fixtures/indexSetFieldTypes';
import type { StubResponse } from 'util/UtilityTypes';

const mockData: StubResponse<typeof SystemIndexSetsTypes.indexSetFieldTypesList> = {
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
      id: 'field_name',
      direction: 'ASC',
    },
  },
  total: 1,
  sort: 'field_name',
  order: 'desc',
  elements: [
    {
      field_name: 'field',
      type: 'bool',
      origin: 'INDEX',
      is_reserved: false,
    },
  ],
};

const expectedState = {
  attributes: [],
  list: [defaultField],
  pagination: {
    total: 1,
  },
};
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));
jest.mock('@graylog/server-api', () => ({
  SystemFieldTypes: {
    getAllFieldTypes: jest.fn(() => Promise.resolve()),
  },
  SystemIndexSetsTypes: {
    indexSetFieldTypesList: jest.fn(),
  },
}));

const renderUseIndexSetFieldTypeHook = () =>
  renderHook(() =>
    useIndexSetFieldType(
      'id-1',
      { page: 1, query: '', pageSize: 10, sort: { attributeId: 'field_name', direction: 'asc' } },
      { enabled: true },
    ),
  );

describe('useIndexSetFieldType custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(SystemIndexSetsTypes.indexSetFieldTypesList).mockResolvedValue(mockData);
    const { result } = renderUseIndexSetFieldTypeHook();

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(SystemIndexSetsTypes.indexSetFieldTypesList).toHaveBeenCalledWith(
      'field_name',
      'id-1',
      undefined,
      undefined,
      1,
      10,
      'asc',
    );

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(SystemIndexSetsTypes.indexSetFieldTypesList).mockRejectedValue(new Error('Error'));

    const { result } = renderUseIndexSetFieldTypeHook();

    await suppressConsole(async () => {
      await waitFor(() => result.current.isLoading);
      await waitFor(() => !result.current.isLoading);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading index field types failed with status: Error: Error',
      'Could not load index field types',
    );
  });
});
