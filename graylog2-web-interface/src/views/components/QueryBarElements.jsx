// @flow strict
import * as React from 'react';

import withPluginEntities from 'views/logic/withPluginEntities';

type Props = {
  queryBarElements: Array<React.ComponentType<{}>>,
};

const QueryBarElements = ({ queryBarElements = [] }) => queryBarElements
// eslint-disable-next-line react/no-array-index-key
  .map((Component, idx) => <Component key={idx} />);

const mapping = {
  queryBarElements: 'views.elements.queryBar',
};

export default withPluginEntities<Props, typeof mapping>(QueryBarElements, mapping);
