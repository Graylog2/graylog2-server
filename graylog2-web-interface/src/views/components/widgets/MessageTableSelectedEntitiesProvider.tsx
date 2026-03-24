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

import * as React from 'react';

import SelectedMessageEntitiesProvider from 'views/components/contexts/SelectedMessageEntitiesProvider';
import type { BackendMessage, Message } from 'views/components/messagelist/Types';

import type { MessageTableBulkSelection, SelectableMessageTableMessage } from './MessageList';
import useSelectableMessageTableMessages from './useSelectableMessageTableMessages';

type Props = React.PropsWithChildren<{
  bulkSelection?: MessageTableBulkSelection;
}>;

const MessageTableSelectedEntitiesProvider = ({ children = null, bulkSelection = undefined }: Props) => {
  const { selectableMessageTableMessages } = useSelectableMessageTableMessages();

  return (
    <SelectedMessageEntitiesProvider<SelectableMessageTableMessage>
      initialSelection={bulkSelection?.initialSelection}
      onChangeSelection={bulkSelection?.onChangeSelection}
      entities={selectableMessageTableMessages}>
      {children}
    </SelectedMessageEntitiesProvider>
  );
};

export const toSelectableMessageTableEntry = (message: Message): BackendMessage => ({
  index: message.index,
  message: {
    ...message.fields,
    _id: message.id,
    timestamp: String(message.fields.timestamp ?? ''),
  },
  highlight_ranges: message.highlight_ranges,
  decoration_stats: message.decoration_stats,
});

export default MessageTableSelectedEntitiesProvider;
