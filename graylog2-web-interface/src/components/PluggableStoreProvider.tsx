import * as React from 'react';
import { useMemo } from 'react';
import { Provider } from 'react-redux';

import usePluginEntities from 'hooks/usePluginEntities';
import createStore from 'store';

const PluggableStoreProvider = ({ children }: React.PropsWithChildren<{}>) => {
  const reducers = usePluginEntities('views.reducers');
  const store = useMemo(() => createStore(reducers), [reducers]);

  return (
    <Provider store={store}>
      {children}
    </Provider>
  );
};

export default PluggableStoreProvider;
