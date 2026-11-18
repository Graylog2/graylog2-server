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
import React, { useCallback, useMemo, useState } from 'react';

import { PaginatedEntityTable } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';
import type { FetchOptions } from 'components/common/PaginatedEntityTable/useFetchEntities';
import useProductName from 'brand-customization/useProductName';
import usePermissions from 'hooks/usePermissions';
import useManagedConfiguration from 'components/datanode/configuration/useManagedConfiguration';

import {
  createColumnDefinitions,
  createColumnRenderers,
  DEFAULT_VISIBLE_COLUMNS,
} from './DataNodesColumnConfiguration';
import { clusterDataNodesKeyFn, fetchClusterDataNodesWithMetrics } from './fetchClusterDataNodes';
import type { ClusterDataNode } from './fetchClusterDataNodes';
import DataNodeRestartBanner from './DataNodeRestartBanner';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number;
};

const DataNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const [totalDataNodes, setTotalDataNodes] = useState<number | undefined>(undefined);
  const productName = useProductName();
  const { isPermitted } = usePermissions();
  // The cluster wide configuration lists every Data Node which still has to be restarted, for its own values too
  const { data: configuration } = useManagedConfiguration(null, isPermitted('clusterconfigentry:read'));
  const pendingNodes = configuration?.pending_nodes;
  const pendingRestartNodeIds = useMemo(() => new Set(pendingNodes ?? []), [pendingNodes]);

  const columnSchemas = useMemo<Array<ColumnSchema>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(
    () => createColumnRenderers(productName, pendingRestartNodeIds),
    [productName, pendingRestartNodeIds],
  );

  const renderActions = useCallback((entity: ClusterDataNode) => <DataNodeActions dataNode={entity} />, []);
  const tableLayout = useMemo(
    () => ({
      entityTableId: 'cluster-data-nodes',
      defaultSort: { attributeId: 'hostname', direction: 'asc' as const },
      defaultDisplayedAttributes: [...DEFAULT_VISIBLE_COLUMNS],
      defaultPageSize: pageSizeLimit ?? 0,
      defaultColumnOrder: [...DEFAULT_VISIBLE_COLUMNS],
    }),
    [pageSizeLimit],
  );
  const externalSearch = useMemo(() => ({ query: searchQuery }), [searchQuery]);
  const fetchOptions = useMemo<FetchOptions>(() => ({ refetchInterval }), [refetchInterval]);

  return (
    <ClusterNodesSectionWrapper
      title="Data Nodes"
      titleCount={totalDataNodes}
      onTitleCountClick={onSelectNodeType ?? null}
      collapsible={collapsible}>
      <DataNodeRestartBanner />
      <PaginatedEntityTable<ClusterDataNode>
        tableLayout={tableLayout}
        fetchEntities={fetchClusterDataNodesWithMetrics}
        keyFn={clusterDataNodesKeyFn}
        additionalAttributes={columnSchemas}
        columnRenderers={columnRenderers}
        entityAttributesAreCamelCase
        entityActions={renderActions}
        humanName="Data Nodes"
        externalSearch={externalSearch}
        fetchOptions={fetchOptions}
        onDataLoaded={(data) => setTotalDataNodes(data.pagination?.total ?? data.list.length)}
        withoutURLParams
      />
    </ClusterNodesSectionWrapper>
  );
};

export default DataNodesExpandable;
