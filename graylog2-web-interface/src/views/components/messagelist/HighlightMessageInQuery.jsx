// @flow strict
import * as React from 'react';

import withLocation from 'routing/withLocation';
import type { Location } from 'routing/withLocation';

import HighlightMessageContext from '../contexts/HighlightMessageContext';

type Props = {
  children: ?React.Node,
  location: Location,
};

const HighlightMessageInQuery = ({ children, location: { query = {} } = {} }: Props) => {
  const { highlightMessage } = query;

  return (
    <HighlightMessageContext.Provider value={highlightMessage}>
      {children}
    </HighlightMessageContext.Provider>
  );
};

HighlightMessageInQuery.propTypes = {};

export default withLocation(HighlightMessageInQuery);
