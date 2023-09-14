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
import useFieldTypes from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypes';

const mockFieldType = {
  string: 'String',
  int: 'Number(int)',
};

const expectedState = {
  fieldTypes: {
    string: 'String',
    int: 'Number(int)',
  },
};
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

const renderUseFieldTypeHook = () => renderHook(() => useFieldTypes());

describe('useFieldType custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve(mockFieldType));
    const { result, waitFor } = renderUseFieldTypeHook();

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(fetch).toHaveBeenCalledWith('GET', qualifyUrl('/system/indices/mappings/types'));

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

    const { result, waitFor } = renderUseFieldTypeHook();

    await suppressConsole(async () => {
      await waitFor(() => result.current.isLoading);
      await waitFor(() => !result.current.isLoading);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading field type options failed with status: Error: Error',
      'Could not load field type options');
  });
});
