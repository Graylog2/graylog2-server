// @flow strict
import React from 'react';
import { trim } from 'lodash';

// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

import { SearchConfigStore } from 'enterprise/stores/SearchConfigStore';
import { getParameterBindingsAsMap } from 'enterprise/logic/search/SearchExecutionState';
import { SearchMetadataStore } from 'enterprise/stores/SearchMetadataStore';
import { SearchExecutionStateStore } from 'enterprise/stores/SearchExecutionStateStore';
import SearchBar from './SearchBar';

const _disableSearch = (undeclaredParameters, parameterBindings, usedParameters) => {
  const bindingsMap = getParameterBindingsAsMap(parameterBindings);
  const missingValues = usedParameters.map(p => bindingsMap.get(p.name)).filter(value => !trim(value));

  return undeclaredParameters.size > 0 || missingValues.size > 0;
};

const SearchBarWithStatus = connect(
  ({ config, isDisabled, onExecute }) => {
    return <SearchBar disableSearch={isDisabled} onExecute={onExecute} config={config} />;
  },
  {
    searchMetadata: SearchMetadataStore,
    executionState: SearchExecutionStateStore,
    configurations: SearchConfigStore,
  },
  ({ searchMetadata, executionState, configurations }) => ({
    isDisabled: _disableSearch(searchMetadata.undeclared, executionState.parameterBindings, searchMetadata.used),
    config: configurations.searchesClusterConfig,
  }),
);

export default SearchBarWithStatus;
