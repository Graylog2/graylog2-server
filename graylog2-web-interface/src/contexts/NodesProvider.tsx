import * as React from 'react';

import NodesContext from 'contexts/NodesContext';
import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';

const NodesProvider = ({ children }: React.PropsWithChildren<{}>) => {
  const value = useStore(NodesStore, ({ nodes }) => nodes);

  return (
    <NodesContext.Provider value={value}>
      {children}
    </NodesContext.Provider>
  );
};

export default NodesProvider;
