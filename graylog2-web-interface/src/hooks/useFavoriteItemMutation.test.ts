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
import { renderHook, act, waitFor } from 'wrappedTestingLibrary/hooks';

import { Favorites } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';
import UserNotification from 'util/UserNotification';
import useUserSearchFilterMutation from 'hooks/useFavoriteItemMutation';

jest.mock('@graylog/server-api', () => ({
  Favorites: { addItemToFavorites: jest.fn(), removeItemFromFavorites: jest.fn() },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('useFavoriteItemMutation', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('putItem to favorites', () => {
    it('should run fetch and display UserNotification', async () => {
      asMock(Favorites.addItemToFavorites).mockImplementation(() => Promise.resolve());
      const { result } = renderHook(() => useUserSearchFilterMutation());

      act(() => {
        result.current.putItem('111');
      });

      await waitFor(() => expect(Favorites.addItemToFavorites).toHaveBeenCalledWith('111'));
    });

    it('should display notification on fail', async () => {
      asMock(Favorites.addItemToFavorites).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result } = renderHook(() => useUserSearchFilterMutation());

      act(() => {
        result.current.putItem('111').catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Adding item to favorites failed with status: Error: Error',
          'Could not add item to favorites',
        ),
      );
    });
  });

  describe('deleteItem from favorites', () => {
    it('should run fetch and display UserNotification', async () => {
      asMock(Favorites.removeItemFromFavorites).mockImplementation(() => Promise.resolve());
      const { result } = renderHook(() => useUserSearchFilterMutation());

      act(() => {
        result.current.deleteItem('111');
      });

      await waitFor(() => expect(Favorites.removeItemFromFavorites).toHaveBeenCalledWith('111'));
    });

    it('should display notification on fail', async () => {
      asMock(Favorites.removeItemFromFavorites).mockImplementation(() => Promise.reject(new Error('Error')));

      const { result } = renderHook(() => useUserSearchFilterMutation());

      act(() => {
        result.current.deleteItem('111').catch(() => {});
      });

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Deleting item from favorites failed with status: Error: Error',
          'Could not delete item from favorites',
        ),
      );
    });
  });
});
