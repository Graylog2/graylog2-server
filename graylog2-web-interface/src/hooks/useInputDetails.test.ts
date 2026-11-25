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
import { waitFor } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import useEntityTitles from 'hooks/useEntityTitles';
import UserNotification from 'util/UserNotification';

import useInputDetails from './useInputDetails';
import type { InputDetailsFetcher } from './useInputDetails';

jest.mock('hooks/usePluginEntities');
jest.mock('hooks/useEntityTitles');
jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
}));

const createFetcher = (fetchDetails: InputDetailsFetcher['fetchDetails']): InputDetailsFetcher => ({
  type: 'forwarder_input',
  fetchDetails,
});

describe('useInputDetails', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useEntityTitles).mockReturnValue({
      titlesById: {},
      notPermittedIds: new Set(),
      isInitialLoading: false,
      isFetching: false,
      isError: false,
    });
  });

  it('resolves the inputs of hooks rendered together with one request per fetcher', async () => {
    const fetchDetails = jest.fn(async (ids: Array<string>) =>
      ids.map((id) => ({ type: 'input' as const, id, title: `Title of ${id}` })),
    );
    asMock(usePluginEntities).mockReturnValue([createFetcher(fetchDetails)]);

    const { result } = renderHook(() => [
      useInputDetails([{ id: 'forwarder-a', type: 'forwarder_input' }]),
      useInputDetails([
        { id: 'forwarder-a', type: 'forwarder_input' },
        { id: 'forwarder-b', type: 'forwarder_input' },
      ]),
    ]);

    await waitFor(() => expect(result.current.every(({ isInitialLoading }) => !isInitialLoading)).toBe(true));

    expect(fetchDetails).toHaveBeenCalledTimes(1);
    expect(fetchDetails).toHaveBeenCalledWith(['forwarder-a', 'forwarder-b']);
    expect(Object.keys(result.current[0].resolvedById)).toEqual(['forwarder-a']);
    expect(Object.keys(result.current[1].resolvedById)).toEqual(['forwarder-a', 'forwarder-b']);
  });

  it('shows one error notification when the request of hooks rendered together fails', async () => {
    const fetchDetails = jest.fn().mockRejectedValue(Object.assign(new Error('Not found'), { status: 404 }));
    asMock(usePluginEntities).mockReturnValue([createFetcher(fetchDetails)]);

    const { result } = renderHook(() => [
      useInputDetails([{ id: 'forwarder-a', type: 'forwarder_input' }]),
      useInputDetails([{ id: 'forwarder-b', type: 'forwarder_input' }]),
    ]);

    await waitFor(() => expect(result.current.every(({ isError }) => isError)).toBe(true));

    expect(UserNotification.error).toHaveBeenCalledTimes(1);
  });
});
