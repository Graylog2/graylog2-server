import React from 'react';
import { act, renderHook } from '@testing-library/react-hooks';
import { QueryClientProvider, QueryClient } from '@tanstack/react-query';

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

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
  logger,
});

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
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
    const putUrl = qualifyUrl(`${urlPrefix}/addToFavorites/111`);

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({}));
      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.putItem('111');
      });

      await waitFor(() => expect(fetch)
        .toHaveBeenCalledWith('PUT', putUrl));

      await waitFor(() => expect(UserNotification.success)
        .toHaveBeenCalledWith('Item added to favorite successfully.', 'Success'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.putItem('111').catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Adding item to favorites failed with status: Error: Error',
        'Could not add item to favorite'));
    });
  });

  describe('deleteItem from favorites', () => {
    const deleteUrl = qualifyUrl(`${urlPrefix}/removeFromFavorites/111`);

    it('should run fetch and display UserNotification', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve());
      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.deleteItem('111');
      });

      await waitFor(() => expect(fetch).toHaveBeenCalledWith('DELETE', deleteUrl));

      await waitFor(() => expect(UserNotification.success)
        .toHaveBeenCalledWith('Item deleted from favorite successfully.', 'Success'));
    });

    it('should display notification on fail', async () => {
      asMock(fetch).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result, waitFor } = renderHook(() => useUserSearchFilterMutation(), { wrapper });

      act(() => {
        result.current.deleteItem('111').catch(() => {});
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith(
        'Deleting item from favorites failed with status: Error: Error',
        'Could not delete item from favorite'));
    });
  });
});
