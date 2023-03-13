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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

export const urlPrefix = '/favorites';

const putFavoriteItem = async (grn: string) => {
  const url = `${urlPrefix}/${grn}`;

  return fetch('PUT', qualifyUrl(url));
};

const deleteFavoriteItem = (grn: string) => {
  const url = `${urlPrefix}/${grn}`;

  return fetch('DELETE', qualifyUrl(url));
};

const useFavoriteItemMutation = () => {
  const putMutation = useMutation(putFavoriteItem, {
    onError: (errorThrown) => {
      UserNotification.error(`Adding item to favorites failed with status: ${errorThrown}`,
        'Could not add item to favorites');
    },
  });

  const deleteMutation = useMutation(deleteFavoriteItem, {
    onError: (errorThrown) => {
      UserNotification.error(`Deleting item from favorites failed with status: ${errorThrown}`,
        'Could not delete item from favorites');
    },
  });

  return { putItem: putMutation.mutateAsync, deleteItem: deleteMutation.mutateAsync };
};

export default useFavoriteItemMutation;
