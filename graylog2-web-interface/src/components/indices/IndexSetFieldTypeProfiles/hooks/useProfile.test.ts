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
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { profile1JSON, profile1 } from 'fixtures/indexSetFieldTypeProfiles';

const mockData = profile1JSON;

const expectedState = profile1;
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('@graylog/server-api', () => ({
  SystemFieldTypes: {
    getAllFieldTypes: jest.fn(() => Promise.resolve()),
  },
}));

const renderUseProfilesHook = () => renderHook(() => useProfile('111'));

describe('useProfiles custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve(mockData));
    const { result, waitFor } = renderUseProfilesHook();

    await waitFor(() => !result.current.isFetched);
    await waitFor(() => result.current.isFetched);

    expect(fetch).toHaveBeenCalledWith('GET', qualifyUrl('/system/indices/index_sets/profiles/111'));

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

    const { result, waitFor } = renderUseProfilesHook();

    await suppressConsole(async () => {
      await waitFor(() => !result.current.isFetched);
      await waitFor(() => result.current.isFetched);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading index field type profile failed with status: Error: Error',
      'Could not load index field type profile');
  });
});
