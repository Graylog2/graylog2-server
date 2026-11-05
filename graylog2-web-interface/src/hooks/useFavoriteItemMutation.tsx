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

import { Favorites } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import i18n from 'i18n';

const putFavoriteItem = async (grn: string) => Favorites.addItemToFavorites(grn);

const deleteFavoriteItem = (grn: string) => Favorites.removeItemFromFavorites(grn);

const useFavoriteItemMutation = () => {
  const putMutation = useMutation({
    mutationFn: putFavoriteItem,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('use_favorite_item_mutation_notifications.put_failed', { ns: 'common', error: errorThrown }),
        i18n.t('use_favorite_item_mutation_notifications.put_failed_title', { ns: 'common' }),
      );
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteFavoriteItem,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('use_favorite_item_mutation_notifications.delete_failed', { ns: 'common', error: errorThrown }),
        i18n.t('use_favorite_item_mutation_notifications.delete_failed_title', { ns: 'common' }),
      );
    },
  });

  return { putItem: putMutation.mutateAsync, deleteItem: deleteMutation.mutateAsync };
};

export default useFavoriteItemMutation;
