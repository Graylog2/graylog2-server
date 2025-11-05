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

import React, { useMemo, useCallback } from 'react';
import zip from 'lodash/zip';
import uniq from 'lodash/uniq';
import flattenDeep from 'lodash/flattenDeep';

import { FavoriteFields } from '@graylog/server-api';

import type { Message } from 'views/components/messagelist/Types';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { Stream } from 'logic/streams/types';
import UserNotification from 'util/UserNotification';
import { StreamsActions } from 'views/stores/StreamsStore';

type OriginalProps = React.PropsWithChildren<{
  message: Message;
  messageFields: FieldTypeMappingsList;
  streams: Array<Stream>;
}>;

const OriginalMessageFavoriteFieldsProvider = ({ children = null, message, messageFields, streams }: OriginalProps) => {
  const initialFavoriteFields = useMemo(
    () => uniq(flattenDeep(zip(streams.map((stream) => stream.favorite_fields)))),
    [streams],
  );

  const saveFavoriteField = useCallback(
    (favoritesToSave: Array<string>) => {
      const newAddedFields = favoritesToSave.filter((f) => !initialFavoriteFields.includes(f));

      const newFavoriteFieldsByStream = Object.fromEntries(
        streams.map((stream) => [
          stream.id,
          favoritesToSave.filter((f) => stream.favorite_fields.includes(f) || newAddedFields.includes(f)),
        ]),
      );

      FavoriteFields.set({ fields: newFavoriteFieldsByStream })
        .then(() => StreamsActions.refresh())
        .catch((errorThrown) =>
          UserNotification.error(
            `Setting fields to favorites failed with error: ${errorThrown}`,
            'Could not set fields to favorites',
          ),
        );
    },
    [initialFavoriteFields, streams],
  );

  const toggleField = useCallback(
    (field: string) => {
      const isFavorite = initialFavoriteFields.includes(field);
      const streamIds = streams.map((stream) => stream.id);

      if (isFavorite) {
        FavoriteFields.remove({ field, stream_ids: streamIds })
          .then(() => StreamsActions.refresh())
          .catch((errorThrown) =>
            UserNotification.error(
              `Removing field from favorites failed with error: ${errorThrown}`,
              'Could not remove fields from favorites',
            ),
          );
      }

      FavoriteFields.add({ field, stream_ids: streamIds })
        .then(() => StreamsActions.refresh())
        .catch((errorThrown) =>
          UserNotification.error(
            `Adding field to favorites failed with error: ${errorThrown}`,
            'Could not add field to favorites',
          ),
        );
    },
    [initialFavoriteFields, streams],
  );

  const contextValue = useMemo(
    () => ({
      favoriteFields: initialFavoriteFields,
      saveFavoriteField,
      messageFields,
      message,
      toggleField,
    }),
    [initialFavoriteFields, saveFavoriteField, messageFields, message, toggleField],
  );

  return <MessageFavoriteFieldsContext.Provider value={contextValue}>{children}</MessageFavoriteFieldsContext.Provider>;
};

const MessageFavoriteFieldsProvider = ({
  children = null,
  message,
  messageFields,
  isFeatureEnabled,
  streams,
}: OriginalProps & { isFeatureEnabled: boolean }) => {
  if (!isFeatureEnabled) return children;

  return (
    <OriginalMessageFavoriteFieldsProvider message={message} messageFields={messageFields} streams={streams}>
      {children}
    </OriginalMessageFavoriteFieldsProvider>
  );
};
export default MessageFavoriteFieldsProvider;
