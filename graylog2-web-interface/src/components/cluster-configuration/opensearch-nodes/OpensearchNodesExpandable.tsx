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

import { PaginatedEntityTable } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import type { FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';

import {
  createColumnDefinitions,
  createColumnRenderers,
  DEFAULT_VISIBLE_COLUMNS,
} from './OpensearchNodesColumnConfiguration';
import type { OpensearchNode, OpensearchNodesResponse } from './fetchClusterOpensearchNodes';
import { clusterOpensearchNodesKeyFn, fetchOpensearchNodes } from './fetchClusterOpensearchNodes';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number;
};

const OpensearchNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const [totalOpensearchNodes, setTotalOpensearchNodes] = useState<number | undefined>(undefined);
  const handleDataLoaded = (data: OpensearchNodesResponse) => {
    setTotalOpensearchNodes(data.pagination?.total ?? data.list.length);
  };

  const columnSchemas: Array<ColumnSchema> = createColumnDefinitions();
  const columnRenderers = createColumnRenderers();

  const tableLayout = {
    entityTableId: 'cluster-opensearch-nodes',
    defaultSort: { attributeId: 'name', direction: 'asc' as const },
    defaultDisplayedAttributes: [...DEFAULT_VISIBLE_COLUMNS],
    defaultPageSize: pageSizeLimit ?? 0,
    defaultColumnOrder: [...DEFAULT_VISIBLE_COLUMNS],
  };
  const externalSearch = { query: searchQuery };
  const fetchOptions: FetchOptions = { refetchInterval };

  return (
    <ClusterNodesSectionWrapper
      title="OpenSearch Nodes"
      titleCount={totalOpensearchNodes}
      onTitleCountClick={onSelectNodeType ?? null}
      collapsible={collapsible}>
      <PaginatedEntityTable<OpensearchNode>
        tableLayout={tableLayout}
        fetchEntities={fetchOpensearchNodes}
        keyFn={clusterOpensearchNodesKeyFn}
        additionalAttributes={columnSchemas}
        columnRenderers={columnRenderers}
        humanName="OpenSearch Nodes"
        externalSearch={externalSearch}
        fetchOptions={fetchOptions}
        onDataLoaded={handleDataLoaded}
        withoutURLParams
        entityAttributesAreCamelCase={false}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default OpensearchNodesExpandable;
