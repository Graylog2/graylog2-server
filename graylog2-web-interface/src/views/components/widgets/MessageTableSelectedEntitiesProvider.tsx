import * as React from 'react';

import SelectedMessageEntitiesProvider from 'views/components/contexts/SelectedMessageEntitiesProvider';
import type { BackendMessage, Message } from 'views/components/messagelist/Types';

import type { MessageTableBulkSelection, SelectableMessageTableMessage } from './MessageList';

type Props = React.PropsWithChildren<{
  bulkSelection?: MessageTableBulkSelection;
  entities: Readonly<Array<SelectableMessageTableMessage>>;
}>;

const MessageTableSelectedEntitiesProvider = ({ children = null, bulkSelection = undefined, entities }: Props) => (
  <SelectedMessageEntitiesProvider<SelectableMessageTableMessage>
    initialSelection={bulkSelection?.initialSelection}
    onChangeSelection={bulkSelection?.onChangeSelection}
    entities={entities}>
    {children}
  </SelectedMessageEntitiesProvider>
);

export const toSelectableMessageTableMessages = (
  messages: Readonly<Array<BackendMessage>>,
  isEntitySelectable: (entity: BackendMessage) => boolean,
): Array<SelectableMessageTableMessage> =>
  messages
    .filter((message) => !!message.message?._id && isEntitySelectable(message))
    .map(({ index, message }) => ({
      id: message._id,
      index,
      timestamp: message.timestamp,
    }));

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
