import * as React from 'react';
import { useMemo } from 'react';
import { Provider } from 'react-redux';

import usePluginEntities from 'hooks/usePluginEntities';
import createStore from 'store';
import type View from 'views/logic/views/View';

type Props = {
  isNew: boolean,
  view: View,
}

const PluggableStoreProvider = ({ children, isNew, view }: React.PropsWithChildren<Props>) => {
  const reducers = usePluginEntities('views.reducers');
  const store = useMemo(() => createStore(reducers, { view: { view, isDirty: false, isNew } }), [reducers, view]);

  console.log({ reducers, store });

  return (
    <Provider store={store}>
      {children}
    </Provider>
  );
};

export default PluggableStoreProvider;
