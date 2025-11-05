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
import { useCallback, useState } from 'react';

import { FavoriteFields } from '@graylog/server-api';

import { StreamsActions } from 'views/stores/StreamsStore';
import UserNotification from 'util/UserNotification';
import type { Stream } from 'logic/streams/types';

const useMessageFavoriteFieldsMutation = (streams: Array<Stream>, initialFavoriteFields: Array<string>) => {
  const [isLoading, setIsLoading] = useState(false);

  const saveFavoriteField = useCallback(
    (favoritesToSave: Array<string>) => {
      const newAddedFields = favoritesToSave.filter((f) => !initialFavoriteFields.includes(f));

      const newFavoriteFieldsByStream = Object.fromEntries(
        streams.map((stream) => [
          stream.id,
          favoritesToSave.filter((f) => stream.favorite_fields.includes(f) || newAddedFields.includes(f)),
        ]),
      );

      setIsLoading(true);
      FavoriteFields.set({ fields: newFavoriteFieldsByStream })
        .then(() => StreamsActions.refresh())
        .catch((errorThrown) =>
          UserNotification.error(
            `Setting fields to favorites failed with error: ${errorThrown}`,
            'Could not set fields to favorites',
          ),
        )
        .finally(() => setIsLoading(false));
    },
    [initialFavoriteFields, streams],
  );

  const toggleField = useCallback(
    (field: string) => {
      const isFavorite = initialFavoriteFields.includes(field);
      const streamIds = streams.map((stream) => stream.id);
      setIsLoading(true);

      if (isFavorite) {
        FavoriteFields.remove({ field, stream_ids: streamIds })
          .then(() => StreamsActions.refresh())
          .catch((errorThrown) =>
            UserNotification.error(
              `Removing field from favorites failed with error: ${errorThrown}`,
              'Could not remove field from favorites',
            ),
          )
          .finally(() => setIsLoading(false));
      } else {
        FavoriteFields.add({ field, stream_ids: streamIds })
          .then(() => StreamsActions.refresh())
          .catch((errorThrown) =>
            UserNotification.error(
              `Adding field to favorites failed with error: ${errorThrown}`,
              'Could not add field to favorites',
            ),
          )
          .finally(() => setIsLoading(false));
      }
    },
    [initialFavoriteFields, streams],
  );

  return {
    isLoading,
    saveFavoriteField,
    toggleField,
  };
};

export default useMessageFavoriteFieldsMutation;
