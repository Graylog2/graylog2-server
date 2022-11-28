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

import { useMutation } from '@tanstack/react-query';
// import { useQueryClient } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const urlPrefix = '/dynamicstartpage';
// const FAVORITE_ITEM_MUTATION = 'FAVORITE_ITEM_MUTATION';

const putFavoriteItem = async (id: string) => {
  const url = `${urlPrefix}/addToFavorites/${id}`;

  return fetch('PUT', qualifyUrl(url));
};

const deleteFavoriteItem = (id: string) => {
  const url = `${urlPrefix}/removeFromFavorites/${id}`;

  return fetch('DELETE', qualifyUrl(url));
};

const useFavoriteItemMutation = () => {
  // const queryClient = useQueryClient();
  const invalidateUserFilters = () => {}; // queryClient.invalidateQueries([FAVORITE_ITEM_MUTATION]);

  const putMutation = useMutation(putFavoriteItem, {
    onSuccess: async () => {
      UserNotification.success('Item added to favorite successfully.', 'Success');
      await invalidateUserFilters();
    },
    onError: (errorThrown) => {
      UserNotification.error(`Adding item to favorites failed with status: ${errorThrown}`,
        'Could not add item to favorite');
    },
  });

  const deleteMutation = useMutation(deleteFavoriteItem, {
    onSuccess: async () => {
      UserNotification.success('Item deleted from favorite successfully.', 'Success');
      await invalidateUserFilters();
    },
    onError: (errorThrown) => {
      UserNotification.error(`Deleting item from favorites failed with status: ${errorThrown}`,
        'Could not delete item from favorite');
    },
  });

  return { putItem: putMutation.mutateAsync, deleteItem: deleteMutation.mutateAsync };
};

export default useFavoriteItemMutation;
