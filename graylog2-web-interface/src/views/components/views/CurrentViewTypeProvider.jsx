// @flow strict
import * as React from 'react';
import connect from 'stores/connect';

import ViewTypeContext from 'views/components/contexts/ViewTypeContext';
import { ViewStore } from 'views/stores/ViewStore';

const CurrentViewTypeProvider = connect(
  ({ type, children }) => <ViewTypeContext.Provider value={type}>{children}</ViewTypeContext.Provider>,
  { view: ViewStore },
  ({ view }) => ({ type: view && view.view ? view.view.type : undefined }),
);

CurrentViewTypeProvider.propTypes = {};

export default CurrentViewTypeProvider;
