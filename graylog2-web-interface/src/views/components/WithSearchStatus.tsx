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
import * as React from 'react';
import { trim } from 'lodash';
import type * as Immutable from 'immutable';

import connect from 'stores/connect';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import type { ParameterBindings } from 'views/logic/search/SearchExecutionState';
import { getParameterBindingsAsMap } from 'views/logic/search/SearchExecutionState';
import { SearchMetadataStore } from 'views/stores/SearchMetadataStore';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import type { SearchesConfig } from 'components/search/SearchConfig';
import type Parameter from 'views/logic/parameters/Parameter';

const _disableSearch = (_undeclaredParameters: Immutable.Set<string>, parameterBindings: ParameterBindings, usedParameters: Immutable.Set<Parameter>) => {
  const bindingsMap = getParameterBindingsAsMap(parameterBindings);
  const missingValues = usedParameters.filter((param) => (param.needsBinding && !param.optional)).map((p) => bindingsMap.get(p.name)).filter((value) => !trim(value));

  return missingValues.size > 0;
};

type SearchStatusProps = {
  config: SearchesConfig;
  disableSearch?: boolean;
}

type WrapperProps = {
  config: SearchesConfig;
  isDisabled: boolean;
};

type ResultProps = {
  config?: SearchesConfig;
  isDisabled?: boolean;
};

const WithSearchStatus = (Component: React.ComponentType<Partial<SearchStatusProps>>): React.ComponentType<ResultProps> => connect(
  ({ config, isDisabled }: WrapperProps) => {
    return <Component disableSearch={isDisabled} config={config} />;
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
