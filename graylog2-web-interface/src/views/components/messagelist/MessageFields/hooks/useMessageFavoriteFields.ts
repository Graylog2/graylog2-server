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
import React from 'react';

export const DEFAULT_FIELDS = ['source', 'destination_ip', 'username'];

const useMessageFavoriteFields = (streams: Array<string>) => {
  const [favorites, setFavorites] = React.useState<Array<string>>(DEFAULT_FIELDS);
  const saveFields = (fields: Array<string>) => {
    // eslint-disable-next-line no-console
    console.log(streams, fields);
    setFavorites(fields);
  };

  return {
    isLoading: false,
    favoriteFields: favorites,
    saveFields,
  };
};

export default useMessageFavoriteFields;
