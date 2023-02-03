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
import { act, renderHook } from '@testing-library/react-hooks';
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import asMock from 'helpers/mocking/AsMock';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import useUserSearchFilterMutation, { urlPrefix } from 'hooks/useFavoriteItemMutation';
import { qualifyUrl } from 'util/URLUtils';

const logger = {
  // eslint-disable-next-line no-console
  log: console.log,
  // eslint-disable-next-line no-console
  warn: console.warn,
  error: () => {},
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <DefaultQueryClientProvider options={{ logger }}>
    {children}
  </DefaultQueryClientProvider>
);

jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useFavoriteItemMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('putItem to favorites', () => {
    const putUrl = qualifyUrl(`${urlPrefix}/111`);

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));
      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.putItem('111');
      });

      await waitFor(() => expect(fetch)
        .toHaveBeenCalledWith('PUT', putUrl));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.putItem('111').catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Adding item to favorites failed with status: Error: Error',
        'Could not add item to favorites'));
    });
  });

  describe('deleteItem from favorites', () => {
    const deleteUrl = qualifyUrl(`${urlPrefix}/111`);

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve());
      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.deleteItem('111');
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('DELETE', deleteUrl));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.deleteItem('111').catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Deleting item from favorites failed with status: Error: Error',
        'Could not delete item from favorites'));
    });
  });
});
