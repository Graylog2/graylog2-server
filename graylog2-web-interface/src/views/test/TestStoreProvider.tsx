import * as React from 'react';

import { createSearch } from 'fixtures/searches';
import PluggableStoreProvider from 'components/PluggableStoreProvider';

const TestStoreProvider = ({ children, ...rest }: React.PropsWithChildren<Partial<React.ComponentProps<typeof PluggableStoreProvider>>>) => {
  const view = rest.view ?? createSearch();
  const isNew = rest.isNew ?? false;
  const initialQuery = rest.initialQuery ?? 'query-id-1';

  return (
    <PluggableStoreProvider view={view} initialQuery={initialQuery} isNew={isNew}>
      {children}
    </PluggableStoreProvider>
  );
};

export default TestStoreProvider;
