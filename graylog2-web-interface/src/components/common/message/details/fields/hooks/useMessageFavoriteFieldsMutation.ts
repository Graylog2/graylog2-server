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
import mapValues from 'lodash/mapValues';

import { FavoriteFields } from '@graylog/server-api';

import { StreamsActions } from 'views/stores/StreamsStore';
import UserNotification from 'util/UserNotification';
import useSendFavoriteFieldTelemetry from 'components/common/message/details/fields/hooks/useSendFavoriteFieldTelemetry';

interface FavoriteFieldRequest {
  readonly field: string;
  readonly stream_ids: string[];
}

interface SetFavoriteFieldsRequest {
  readonly fields: {
    readonly [_key: string]: string[];
  };
}

const useMessageFavoriteFieldsMutation = (
  initialFavoriteFieldsByStream: Record<string, Array<string>>,
  initialFavoriteFields: Array<string>,
) => {
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

      const newFavoriteFieldsByStream = mapValues(initialFavoriteFieldsByStream, (streamFavoriteFields) =>
        favoritesToSave.filter((f) => streamFavoriteFields.includes(f) || newAddedFields.includes(f)),
      );

      setFavoriteFields({ fields: newFavoriteFieldsByStream });
    },
    [initialFavoriteFields, initialFavoriteFieldsByStream, setFavoriteFields],
  );

  const toggleField = useCallback(
    (field: string) => {
      const isFavorite = initialFavoriteFields?.includes(field);
      const streamIds = Object.keys(initialFavoriteFieldsByStream);

      if (isFavorite) {
        removeFavoriteField({ field, stream_ids: streamIds });
      } else {
        addFavoriteField({ field, stream_ids: streamIds });
      }
    },
    [addFavoriteField, initialFavoriteFields, initialFavoriteFieldsByStream, removeFavoriteField],
  );

  return {
    setFieldsIsPending,
    saveFavoriteField,
    toggleField,
  };
};

export default useMessageFavoriteFieldsMutation;
