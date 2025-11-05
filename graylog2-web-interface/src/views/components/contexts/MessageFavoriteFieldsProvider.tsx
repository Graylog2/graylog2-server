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

import React, { useMemo } from 'react';
import zip from 'lodash/zip';
import uniq from 'lodash/uniq';
import flattenDeep from 'lodash/flattenDeep';

import type { Message } from 'views/components/messagelist/Types';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { Stream } from 'logic/streams/types';
import useMessageFavoriteFieldsMutation from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsMutation';

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
  const { saveFavoriteField, toggleField, isLoading } = useMessageFavoriteFieldsMutation(
    streams,
    initialFavoriteFields,
  );

  const contextValue = useMemo(
    () => ({
      isLoading,
      favoriteFields: initialFavoriteFields,
      saveFavoriteField,
      messageFields,
      message,
      toggleField,
    }),
    [isLoading, initialFavoriteFields, saveFavoriteField, messageFields, message, toggleField],
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
