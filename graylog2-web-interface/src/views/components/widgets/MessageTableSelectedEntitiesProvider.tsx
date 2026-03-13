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
