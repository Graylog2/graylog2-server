// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { useStore } from 'stores/connect';
import { HighlightingRulesStore } from 'views/stores/HighlightingRulesStore';
import HighlightingRulesContext from './HighlightingRulesContext';

const HighlightingRulesProvider = ({ children }: { children: React.Node }) => {
  const highlightingRules = useStore(HighlightingRulesStore);
  return highlightingRules
    ? (
      <HighlightingRulesContext.Provider value={highlightingRules}>
        {children}
      </HighlightingRulesContext.Provider>
    )
    : children;
};

HighlightingRulesProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export default HighlightingRulesProvider;
