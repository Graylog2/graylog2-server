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
