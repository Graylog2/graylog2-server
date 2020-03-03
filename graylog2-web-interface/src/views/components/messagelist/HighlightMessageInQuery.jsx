// @flow strict
import * as React from 'react';
import HighlightMessageContext from '../contexts/HighlightMessageContext';

type Props = {
  children: ?React.Node,
  query: { [string]: string },
};

const HighlightMessageInQuery = ({ children, query = {}}: Props) => {
  const { highlightMessage } = query;
  return (
    <HighlightMessageContext.Provider value={highlightMessage}>
      {children}
    </HighlightMessageContext.Provider>
  );
};

HighlightMessageInQuery.propTypes = {};

export default HighlightMessageInQuery;
