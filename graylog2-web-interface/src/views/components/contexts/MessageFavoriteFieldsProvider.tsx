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
import useMessageFavoriteFieldsMutation from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsMutation';
import { useStore } from 'stores/connect';
import { StreamsStore } from 'views/stores/StreamsStore';
import type { Stream } from 'logic/streams/types';
import { DEFAULT_FIELDS } from 'views/components/messagelist/MessageFields/hooks/useMessageFavoriteFieldsForEditing';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';

type OriginalProps = React.PropsWithChildren<{
  message: Message;
  messageFields: FieldTypeMappingsList;
}>;

const OriginalMessageFavoriteFieldsProvider = ({ children = null, message, messageFields }: OriginalProps) => {
  const { streams: streamsList = [] } = useStore(StreamsStore);
  const { permissions } = useCurrentUser();
  const streams = useMemo<Array<Stream>>(() => {
    const messageStreamIds: Array<string> = message?.fields?.streams ?? [];
    const streamsById = Object.fromEntries(
      streamsList
        .filter((stream: Stream) => isPermitted(permissions, `streams:read:${stream.id}`))
        .map((stream) => [stream.id, stream]),
    );

    return messageStreamIds.map((id) => streamsById?.[id]).filter((s) => !!s);
  }, [message?.fields?.streams, permissions, streamsList]);

  const initialFavoriteFields = useMemo(
    () => uniq(flattenDeep(zip(streams.map((stream) => stream?.favorite_fields ?? DEFAULT_FIELDS)))),
    [streams],
  );

  const editableStreams = useMemo(
    () => streams.filter((stream: Stream) => isPermitted(permissions, `streams:edit:${stream.id}`)),
    [permissions, streams],
  );

  const { saveFavoriteField, toggleField, setFieldsIsPending } = useMessageFavoriteFieldsMutation(
    editableStreams,
    initialFavoriteFields,
  );

  const contextValue = useMemo(
    () => ({
      favoriteFields: initialFavoriteFields,
      saveFavoriteField,
      messageFields,
      message,
      toggleField,
      editableStreams,
      setFieldsIsPending,
    }),
    [
      initialFavoriteFields,
      saveFavoriteField,
      messageFields,
      message,
      toggleField,
      editableStreams,
      setFieldsIsPending,
    ],
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
