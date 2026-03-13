import { useContext } from 'react';

import SelectableMessageTableMessagesContext from './SelectableMessageTableMessagesContext';

const useSelectableMessageTableMessages = () => {
  const contextValue = useContext(SelectableMessageTableMessagesContext);

  if (!contextValue) {
    throw new Error(
      'useSelectableMessageTableMessages needs to be used inside SelectableMessageTableMessagesContext.Provider',
    );
  }

  return contextValue;
};

export default useSelectableMessageTableMessages;
