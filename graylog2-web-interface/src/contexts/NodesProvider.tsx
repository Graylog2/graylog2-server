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
import isEqual from 'lodash/isEqual';

import NodesContext from 'contexts/NodesContext';
import { useStore } from 'stores/connect';
import { NodesStore } from 'stores/nodes/NodesStore';

const MemoNodesProvider = React.memo(({ children, value }: React.PropsWithChildren<{ value: React.ComponentProps<typeof NodesContext.Provider>['value'] }>) => (
  <NodesContext.Provider value={value}>
    {children}
  </NodesContext.Provider>
), isEqual);

const NodesProvider = ({ children }: React.PropsWithChildren<{}>) => {
  const value = useStore(NodesStore, ({ nodes }) => Object.fromEntries(
    Object.entries(nodes ?? {}).map(([id, { short_node_id, hostname }]) => [id, { id, short_node_id, hostname }]),
  ));

  return (
    <MemoNodesProvider value={value}>
      {children}
    </MemoNodesProvider>
  );
};

export default NodesProvider;
