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
import useProfiles from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfiles';
import { profile1JSON, profile2JSON, profile1, profile2, indexSetsAttribute } from 'fixtures/indexSetFieldTypeProfiles';
import type { StubResponse } from 'util/UtilityTypes';

const mockData: StubResponse<typeof SystemIndexSetsFieldTypeProfiles.getPage> = {
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
      id: 'name',
      direction: 'ASC',
    },
  },
  total: 1,
  sort: 'name',
  order: 'desc',
  elements: [profile1JSON, profile2JSON],
};

const expectedState = {
  attributes: [indexSetsAttribute],
  list: [profile1, profile2],
  pagination: {
    total: 1,
  },
};
jest.mock('util/UserNotification', () => ({ error: jest.fn() }));
jest.mock('@graylog/server-api', () => ({
  SystemFieldTypes: {
    getAllFieldTypes: jest.fn(() => Promise.resolve()),
  },
  SystemIndexSetsFieldTypeProfiles: {
    getPage: jest.fn(),
  },
}));

const renderUseProfilesHook = () =>
  renderHook(() =>
    useProfiles(
      { page: 1, query: '', pageSize: 10, sort: { attributeId: 'name', direction: 'asc' } },
      { enabled: true },
    ),
  );

describe('useProfiles custom hook', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('Test return initial data and take from fetch', async () => {
    asMock(SystemIndexSetsFieldTypeProfiles.getPage).mockResolvedValue(mockData);
    const { result } = renderUseProfilesHook();

    await waitFor(() => result.current.isLoading);
    await waitFor(() => !result.current.isLoading);

    expect(SystemIndexSetsFieldTypeProfiles.getPage).toHaveBeenCalledWith('name', 1, 10, undefined, undefined, 'asc');

    expect(result.current.data).toEqual(expectedState);
  });

  it('Test trigger notification on fail', async () => {
    asMock(SystemIndexSetsFieldTypeProfiles.getPage).mockRejectedValue(new Error('Error'));

    const { result } = renderUseProfilesHook();

    await suppressConsole(async () => {
      await waitFor(() => result.current.isLoading);
      await waitFor(() => !result.current.isLoading);
    });

    expect(UserNotification.error).toHaveBeenCalledWith(
      'Loading index field type profiles failed with status: Error: Error',
      'Could not load index field type profiles',
    );
  });
});
