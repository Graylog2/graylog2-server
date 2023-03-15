import * as React from 'react';
import type { Subtract } from 'utility-types';

import type { HistoryFunction } from './useHistory';
import useHistory from './useHistory';

type HistoryContext = {
  history: HistoryFunction;
};

const withParams = <Props extends HistoryContext>(Component: React.ComponentType<Props>): React.ComponentType<Subtract<Props, HistoryContext>> => (props) => {
  const history = useHistory();

  return <Component {...props as Props} history={history} />;
};

export default withParams;
