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

import type { Message } from 'views/components/messagelist/Types';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import useMessageFavoriteFields from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFields';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';

type OriginalProps = React.PropsWithChildren<{
  message: Message;
  messageFields: FieldTypeMappingsList;
}>;

const OriginalMessageFavoriteFieldsProvider = ({ children = null, message, messageFields }: OriginalProps) => {
  const {
    isLoading,
    saveFields,
    favoriteFields: initialFavoriteFields,
  } = useMessageFavoriteFields(message.fields.streams);

  const saveFavoriteField = useCallback(
    (favoritesToSave: Array<string>) => {
      saveFields(favoritesToSave);
    },
    [saveFields],
  );

  const contextValue = useMemo(
    () => ({
      isLoadingFavoriteFields: isLoading,
      favoriteFields: initialFavoriteFields,
      saveFavoriteField,
      messageFields,
      message,
    }),
    [isLoading, initialFavoriteFields, saveFavoriteField, messageFields, message],
  );

  return <MessageFavoriteFieldsContext.Provider value={contextValue}>{children}</MessageFavoriteFieldsContext.Provider>;
};

const MessageFavoriteFieldsProvider = ({
  children = null,
  message,
  messageFields,
  isFeatureEnabled,
}: OriginalProps & { isFeatureEnabled: boolean }) => {
  if (!isFeatureEnabled) return children;

  return (
    <OriginalMessageFavoriteFieldsProvider message={message} messageFields={messageFields}>
      {children}
    </OriginalMessageFavoriteFieldsProvider>
  );
};
export default MessageFavoriteFieldsProvider;
