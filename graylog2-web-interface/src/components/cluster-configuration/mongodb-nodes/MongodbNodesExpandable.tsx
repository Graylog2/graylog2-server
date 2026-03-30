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
import React, { useState } from 'react';

import { IfPermitted, PaginatedEntityTable } from 'components/common';
import type { FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';

import { createColumnRenderers, DEFAULT_VISIBLE_COLUMNS } from './MongodbNodesColumnConfiguration';
import type { MongodbNode, MongodbNodesResponse } from './fetchClusterMongodbNodes';
import { clusterMongodbNodesKeyFn, fetchMongodbNodes } from './fetchClusterMongodbNodes';
import MongodbProfilingAction from './MongodbProfilingAction';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

const MONGODB_ENABLE_PROFILING_PERMISSION = 'mongodb:enableprofiling';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number;
};

const MongodbNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const [totalMongodbNodes, setTotalMongodbNodes] = useState<number | undefined>(undefined);
  const handleDataLoaded = (data: MongodbNodesResponse) => {
    setTotalMongodbNodes(data.pagination?.total ?? data.list.length);
  };

  const columnRenderers = createColumnRenderers();

  const tableLayout = {
    entityTableId: 'cluster-mongodb-nodes',
    defaultSort: { attributeId: 'name', direction: 'asc' as const },
    defaultDisplayedAttributes: [...DEFAULT_VISIBLE_COLUMNS],
    defaultPageSize: pageSizeLimit ?? 0,
    defaultColumnOrder: [...DEFAULT_VISIBLE_COLUMNS],
  };
  const externalSearch = { query: searchQuery };
  const fetchOptions: FetchOptions = { refetchInterval };

  return (
    <ClusterNodesSectionWrapper
      title="MongoDB Nodes"
      titleCount={totalMongodbNodes}
      onTitleCountClick={onSelectNodeType ?? null}
      collapsible={collapsible}>
      <IfPermitted permissions={MONGODB_ENABLE_PROFILING_PERMISSION}>
        <MongodbProfilingAction />
      </IfPermitted>
      <PaginatedEntityTable<MongodbNode>
        tableLayout={tableLayout}
        fetchEntities={fetchMongodbNodes}
        keyFn={clusterMongodbNodesKeyFn}
        columnRenderers={columnRenderers}
        humanName="MongoDB Nodes"
        externalSearch={externalSearch}
        fetchOptions={fetchOptions}
        onDataLoaded={handleDataLoaded}
        withoutURLParams
        entityAttributesAreCamelCase={false}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default MongodbNodesExpandable;
