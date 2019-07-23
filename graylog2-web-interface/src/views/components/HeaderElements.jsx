// @flow strict
import * as React from 'react';

import withPluginEntities from 'views/logic/withPluginEntities';

type Props = {
  headerElements: Array<React.ComponentType<{}>>,
};

const HeaderElements = ({ headerElements = [] }: Props) => headerElements
// eslint-disable-next-line react/no-array-index-key
  .map((Component, idx) => <Component key={idx} />);

const mapping = {
  headerElements: 'views.elements.header',
};

export default withPluginEntities<Props, typeof mapping>(HeaderElements, mapping);
