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

import { SystemIndexSetsFieldTypeProfiles } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import suppressConsole from 'helpers/suppressConsole';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';

const mockData = [
  { name: 'Profile 1', id: '111' },
  { name: 'Profile 2', id: '222' },
];

const expectedState = [
  { label: 'Profile 1', value: '111' },
  { label: 'Profile 2', value: '222' },
];
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));
jest.mock('@graylog/server-api', () => ({
  SystemIndexSetsFieldTypeProfiles: { getAll: jest.fn(() => Promise.resolve()) },
}));

const renderUseProfileOptions = () => renderHook(() => useProfileOptions());

describe('useProfileOptions custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(SystemIndexSetsFieldTypeProfiles.getAll).mockImplementation(() => Promise.resolve(mockData));
    const { result } = renderUseProfileOptions();

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(SystemIndexSetsFieldTypeProfiles.getAll).toHaveBeenCalled();

    expect(result.current.options).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(SystemIndexSetsFieldTypeProfiles.getAll).mockImplementation(() => Promise.reject(new Error('Error')));

    const { result } = renderUseProfileOptions();

    await suppressConsole(async () => {
      await waitFor(() => result.current.isLoading);
      await waitFor(() => !result.current.isLoading);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading index field type profile options failed with status: Error: Error',
      'Could not load index field type profile options',
    );
  });
});
