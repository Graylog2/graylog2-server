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

import type { Message } from 'views/components/messagelist/Types';
import MessageFavoriteFieldsContext from 'views/components/contexts/MessageFavoriteFieldsContext';
import useMessageFavoriteFields from 'views/hooks/useMessageFavoriteFields';

const MessageFavoriteFieldsProvider = ({
  children = null,
  message,
}: React.PropsWithChildren<{
  message: Message;
}>) => {
  const [isEditMode, setIsEditMode] = React.useState(false);
  const { removeFromFavoriteFields, favoriteFields, addToFavoriteFields, isLoading } = useMessageFavoriteFields(
    message.stream_ids,
  );

  const contextValue = useMemo(
    () => ({
      setIsEditMode,
      isEditMode,
      isLoadingFavoriteFields: isLoading,
      addToFavoriteFields,
      favoriteFields,
      removeFromFavoriteFields,
    }),
    [addToFavoriteFields, favoriteFields, isEditMode, isLoading, removeFromFavoriteFields],
  );

  return <MessageFavoriteFieldsContext.Provider value={contextValue}>{children}</MessageFavoriteFieldsContext.Provider>;
};

export default MessageFavoriteFieldsProvider;
