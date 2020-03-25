// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import connect from 'stores/connect';

import { ViewStore } from 'views/stores/ViewStore';
import type { ViewType } from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

type Props = {
  type: ?ViewType,
  children: React.Node,
};

const CurrentViewTypeProvider = ({ type, children }: Props) => <ViewTypeContext.Provider value={type}>{children}</ViewTypeContext.Provider>;

CurrentViewTypeProvider.propTypes = {
  type: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
};

export default connect(
  CurrentViewTypeProvider,
  { view: ViewStore },
  ({ view }) => ({ type: (view && view.view) ? view.view.type : undefined }),
);
