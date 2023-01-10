import * as React from 'react';

import { createSearch } from 'fixtures/searches';
import PluggableStoreProvider from 'components/PluggableStoreProvider';

const TestStoreProvider = ({ children }: React.PropsWithChildren<{}>) => {
  const view = createSearch();

  return (
    <PluggableStoreProvider view={view} initialQuery="query-id-1" isNew={false}>
      {children}
    </PluggableStoreProvider>
  );
};

export default TestStoreProvider;
