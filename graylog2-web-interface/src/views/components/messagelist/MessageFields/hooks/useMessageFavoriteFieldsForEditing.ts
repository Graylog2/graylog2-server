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
import { useState, useCallback, useContext } from 'react';
import uniq from 'lodash/uniq';

import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import type { FormattedField } from 'views/components/messagelist/MessageFields/types';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

export const DEFAULT_FIELDS = ['source', 'destination_ip', 'username'];

const useMessageFavoriteFieldsForEditing = () => {
  const sendTelemetry = useSendTelemetry();
  const { saveFavoriteField, favoriteFields: initialFavoriteFields } = useContext(MessageFavoriteFieldsContext);
  const [favorites, setFavorites] = useState<Array<string>>(initialFavoriteFields ?? []);
  const resetFavoriteFields = useCallback(() => {
    setFavorites(DEFAULT_FIELDS);
    saveFavoriteField(DEFAULT_FIELDS);
  }, [saveFavoriteField]);
  const saveEditedFavoriteFields = useCallback(() => {
    saveFavoriteField(favorites);
  }, [favorites, saveFavoriteField]);
  const reorderFavoriteFields = useCallback(
    (items: Array<FormattedField>) => {
      sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_WIDGET_ACTION.CHANGE_MESSAGE_FAVORITE_FIELD_REORDERED, {
        app_action_value: 'reordered',
      });
      setFavorites(items.map((item: FormattedField) => item.field));
    },
    [sendTelemetry],
  );
  const onFavoriteToggle = useCallback(
    (fieldName: string) =>
      setFavorites((favs) => {
        const isFavorite = favs.includes(fieldName);

        if (isFavorite) return favs.filter((fav) => fav !== fieldName);

        return uniq([...favs, fieldName]);
      }),
    [setFavorites],
  );

  return {
    editingFavoriteFields: favorites,
    resetFavoriteFields,
    saveEditedFavoriteFields,
    reorderFavoriteFields,
    onFavoriteToggle,
  };
};

export default useMessageFavoriteFieldsForEditing;
