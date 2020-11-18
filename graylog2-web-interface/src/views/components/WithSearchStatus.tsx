/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
