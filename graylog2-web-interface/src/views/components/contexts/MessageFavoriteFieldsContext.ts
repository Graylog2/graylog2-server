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
import Immutable from 'immutable';

import { singleton } from 'logic/singleton';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { Message } from 'views/components/messagelist/Types';
import type { Stream } from 'logic/streams/types';

export type MessageFavoriteFieldsContextState = {
  favoriteFields: Array<string>;
  saveFavoriteField: (favorites: Array<string>) => void;
  messageFields: FieldTypeMappingsList;
  toggleField: (field: string) => void;
  message: Message;
  editableStreams: Array<Stream>;
  setFieldsIsPending: boolean;
};

const MessageFavoriteFieldsContext = React.createContext<MessageFavoriteFieldsContextState>({
  favoriteFields: [],
  saveFavoriteField: () => {},
  messageFields: Immutable.List([]),
  toggleField: () => {},
  message: undefined,
  editableStreams: [],
  setFieldsIsPending: false,
});

export default singleton('contexts.MessageFavoriteFieldsContext', () => MessageFavoriteFieldsContext);
