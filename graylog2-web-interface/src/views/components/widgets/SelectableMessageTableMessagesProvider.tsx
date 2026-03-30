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
import { useMemo } from 'react';

import type { BackendMessage } from 'views/components/messagelist/Types';

import type { SelectableMessageTableMessage } from './MessageList';
import SelectableMessageTableMessagesContext from './SelectableMessageTableMessagesContext';

type Props = React.PropsWithChildren<{
  displayBulkSelectCol?: boolean;
  isEntitySelectable?: (message: BackendMessage) => boolean;
  messages: Readonly<Array<BackendMessage>>;
}>;

const defaultIsEntitySelectable = () => true;

const SelectableMessageTableMessagesProvider = ({
  children = null,
  displayBulkSelectCol = false,
  isEntitySelectable = defaultIsEntitySelectable,
  messages,
}: Props) => {
  const contextValue = useMemo(() => {
    if (!displayBulkSelectCol) {
      return { selectableMessageTableMessages: [] };
    }

    return {
      selectableMessageTableMessages: messages
        .filter((message) => !!message.message?._id && isEntitySelectable(message))
        .map(
          ({ index, message }): SelectableMessageTableMessage => ({
            id: message._id,
            index,
            timestamp: message.timestamp,
          }),
        ),
    };
  }, [displayBulkSelectCol, isEntitySelectable, messages]);

  return (
    <SelectableMessageTableMessagesContext.Provider value={contextValue}>
      {children}
    </SelectableMessageTableMessagesContext.Provider>
  );
};

export default SelectableMessageTableMessagesProvider;
