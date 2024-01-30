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
import UserNotification from 'util/UserNotification';
import suppressConsole from 'helpers/suppressConsole';
import useFieldTypesForMappings from 'views/logic/fieldactions/ChangeFieldType/hooks/useFieldTypesForMappings';
import { SystemFieldTypes } from '@graylog/server-api';

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
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));

jest.mock('@graylog/server-api', () => ({
  SystemFieldTypes: {
    getAllFieldTypes: jest.fn(() => Promise.resolve()),
  },
}));

const renderUseFieldTypeHook = () => renderHook(() => useFieldTypesForMappings());

describe('useFieldType custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(SystemFieldTypes.getAllFieldTypes).mockImplementation(() => Promise.resolve(mockFieldType));
    const { result, waitFor } = renderUseFieldTypeHook();

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(SystemFieldTypes.getAllFieldTypes).toHaveBeenCalledWith();

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(SystemFieldTypes.getAllFieldTypes).mockImplementation(() => Promise.reject(new Error('Error')));

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
