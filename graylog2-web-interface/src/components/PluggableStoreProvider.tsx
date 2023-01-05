import * as React from 'react';
import { useMemo } from 'react';
import { Provider } from 'react-redux';
import * as Immutable from 'immutable';

import usePluginEntities from 'hooks/usePluginEntities';
import createStore from 'store';
import type View from 'views/logic/views/View';
import type { QueryId } from 'views/logic/queries/Query';
import type { QuerySet } from 'views/logic/search/Search';

type Props = {
  initialQuery: QueryId,
  isNew: boolean,
  view: View,
}

const PluggableStoreProvider = ({ initialQuery, children, isNew, view }: React.PropsWithChildren<Props>) => {
  const reducers = usePluginEntities('views.reducers');
  const activeQuery = useMemo(() => {
    if (initialQuery) {
      return initialQuery;
    }

    const queries: QuerySet = view?.search?.queries ?? Immutable.Set();

    return queries.first()?.id;
  }, [initialQuery, view?.search?.queries]);
  const store = useMemo(() => createStore(reducers, { view: { view, isDirty: false, isNew, activeQuery } }), [activeQuery, isNew, reducers, view]);

  return (
    <Provider store={store}>
      {children}
    </Provider>
  );
};

export default PluggableStoreProvider;
