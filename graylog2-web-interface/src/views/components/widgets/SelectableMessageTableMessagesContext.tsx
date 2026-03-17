import * as React from 'react';

import { singleton } from 'logic/singleton';
import type { SelectableMessageTableMessage } from 'views/components/widgets/MessageList';

export interface SelectableMessageTableMessagesState {
  selectableMessageTableMessages: Array<SelectableMessageTableMessage>;
}

const SelectableMessageTableMessagesContext = React.createContext<SelectableMessageTableMessagesState | undefined>(
  undefined,
);

export default singleton(
  'contexts.views.SelectableMessageTableMessagesContext',
  () => SelectableMessageTableMessagesContext,
);
