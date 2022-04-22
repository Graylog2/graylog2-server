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

import connect from 'stores/connect';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import type { SearchesConfig } from 'components/search/SearchConfig';

type SearchStatusProps = {
  config: SearchesConfig;
}

type WrapperProps = {
  config: SearchesConfig;
};

type ResultProps = {
  config?: SearchesConfig;
};

const WithSearchStatus = (Component: React.ComponentType<Partial<SearchStatusProps>>): React.ComponentType<ResultProps> => connect(
  ({ config }: WrapperProps) => {
    return <Component config={config} />;
  },
  { configurations: SearchConfigStore },
  ({ configurations }) => ({ config: configurations.searchesClusterConfig }),
);

export default WithSearchStatus;
