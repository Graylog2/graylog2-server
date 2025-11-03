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

import { DEFAULT_FIELDS } from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFields';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';

const useMessageFavoriteFieldsForEditing = () => {
  const { saveFavoriteField, favoriteFields: initialFavoriteFields } = useContext(MessageFavoriteFieldsContext);
  const [favorites, setFavorites] = useState<Array<string>>(initialFavoriteFields ?? []);
  const resetFavoriteField = useCallback(() => {
    setFavorites(DEFAULT_FIELDS);
    saveFavoriteField(DEFAULT_FIELDS);
  }, [saveFavoriteField]);
  const saveEditedFavoriteFields = useCallback(() => {
    saveFavoriteField(favorites);
  }, [favorites, saveFavoriteField]);

  return {
    editingFavoriteFields: favorites,
    resetFavoriteField,
    saveEditedFavoriteFields,
    setFavorites,
  };
};

export default useMessageFavoriteFieldsForEditing;
