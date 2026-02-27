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
import { useCallback } from 'react';
import { useMutation } from '@tanstack/react-query';

import { FavoriteFields } from '@graylog/server-api';

import { StreamsActions } from 'views/stores/StreamsStore';
import UserNotification from 'util/UserNotification';
import type { Stream } from 'logic/streams/types';
import useSendFavoriteFieldTelemetry from 'views/components/messagelist/MessageFields/hooks/useSendFavoriteFieldTelemetry';

interface FavoriteFieldRequest {
  readonly field: string;
  readonly stream_ids: string[];
}

interface SetFavoriteFieldsRequest {
  readonly fields: {
    readonly [_key: string]: string[];
  };
}

const useMessageFavoriteFieldsMutation = (streams: Array<Stream>, initialFavoriteFields: Array<string>) => {
  const sendFavoriteFieldTelemetry = useSendFavoriteFieldTelemetry();
  const { isPending: setFieldsIsPending, mutate: setFavoriteFields } = useMutation({
    mutationFn: (props: SetFavoriteFieldsRequest) => FavoriteFields.set(props),
    onSuccess: (_, newFavoriteFieldsByStream) => {
      sendFavoriteFieldTelemetry('EDIT_SAVED', {
        fields_lengths: Object.values(newFavoriteFieldsByStream.fields).map((fields) => fields.length),
      });

      return StreamsActions.refresh();
    },
    onError: (errorThrown) =>
      UserNotification.error(
        `Setting fields to favorites failed with error: ${errorThrown}`,
        'Could not set fields to favorites',
      ),
  });

  const { mutate: addFavoriteField } = useMutation({
    mutationFn: (props: FavoriteFieldRequest) => FavoriteFields.add(props),
    onSuccess: () => {
      sendFavoriteFieldTelemetry('TOGGLED', {
        app_action_value: 'add',
      });

      return StreamsActions.refresh();
    },
    onError: (errorThrown) =>
      UserNotification.error(
        `Adding field to favorites failed with error: ${errorThrown}`,
        'Could not add field to favorites',
      ),
  });

  const { mutate: removeFavoriteField } = useMutation({
    mutationFn: (props: FavoriteFieldRequest) => FavoriteFields.remove(props),
    onSuccess: () => {
      sendFavoriteFieldTelemetry('TOGGLED', {
        app_action_value: 'remove',
      });

      return StreamsActions.refresh();
    },
    onError: (errorThrown) =>
      UserNotification.error(
        `Removing field from favorites failed with error: ${errorThrown}`,
        'Could not remove field from favorites',
      ),
  });

  const saveFavoriteField = useCallback(
    (favoritesToSave: Array<string>) => {
      const newAddedFields = favoritesToSave.filter((f) => !initialFavoriteFields.includes(f));

      const newFavoriteFieldsByStream = Object.fromEntries(
        streams.map((stream) => [
          stream.id,
          favoritesToSave.filter((f) => {
            const streamFavoriteFields = stream?.favorite_fields ?? [];

            return streamFavoriteFields.includes(f) || newAddedFields.includes(f);
          }),
        ]),
      );

      setFavoriteFields({ fields: newFavoriteFieldsByStream });
    },
    [initialFavoriteFields, setFavoriteFields, streams],
  );

  const toggleField = useCallback(
    (field: string) => {
      const isFavorite = initialFavoriteFields?.includes(field);
      const streamIds = streams.map((stream) => stream.id);

      if (isFavorite) {
        removeFavoriteField({ field, stream_ids: streamIds });
      } else {
        addFavoriteField({ field, stream_ids: streamIds });
      }
    },
    [addFavoriteField, initialFavoriteFields, removeFavoriteField, streams],
  );

  return {
    setFieldsIsPending,
    saveFavoriteField,
    toggleField,
  };
};

export default useMessageFavoriteFieldsMutation;
