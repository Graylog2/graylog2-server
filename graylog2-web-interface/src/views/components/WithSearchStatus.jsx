// @flow strict
import * as React from 'react';
import { trim } from 'lodash';

import connect from 'stores/connect';

import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import { getParameterBindingsAsMap } from 'views/logic/search/SearchExecutionState';
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';

const _disableSearch = (undeclaredParameters, parameterBindings, usedParameters) => {
  const bindingsMap = getParameterBindingsAsMap(parameterBindings);
  const missingValues = usedParameters.map((p) => bindingsMap.get(p.name)).filter((value) => !trim(value));

  return undeclaredParameters.size > 0 || missingValues.size > 0;
};

const WithSearchStatus = (Component: React.AbstractComponent<any>) => connect(
  ({ config, isDisabled, onExecute }) => {
    return <Component disableSearch={isDisabled} onExecute={onExecute} config={config} />;
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

export default WithSearchStatus;
