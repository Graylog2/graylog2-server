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
import React, { useCallback, useMemo } from 'react';

import { EntityDataTable, Spinner } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';

import {
  createColumnDefinitions,
  createColumnRenderers,
} from './DataNodesColumnConfiguration';
import useClusterDataNodesTableLayout from './useClusterDataNodesTableLayout';
import useClusterDataNodes, { type ClusterDataNode } from './useClusterDataNodes';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectSegment?: () => void;
  pageSizeLimit?: number;
};

const DataNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectSegment = undefined,
  pageSizeLimit = undefined,
}: Props) => {
  const {
    columnsOrder,
    columnPreferences,
    defaultDisplayedColumns,
    searchParams,
    isLoadingLayout,
    handleColumnPreferencesChange,
    handleSortChange,
  } = useClusterDataNodesTableLayout(searchQuery, pageSizeLimit);
  const {
    nodes: dataNodes,
    total: totalDataNodes,
    refetch,
    isLoading,
    setPollingEnabled,
  } = useClusterDataNodes(searchParams);

  const columnSchemas = useMemo<Array<ColumnSchema>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const handleActionsInteractionChange = useCallback(
    (isActive: boolean) => {
      setPollingEnabled(!isActive);
    },
    [setPollingEnabled],
  );

  const renderActions = useCallback(
    (entity: ClusterDataNode) => (
      <DataNodeActions dataNode={entity} refetch={refetch} onInteractionChange={handleActionsInteractionChange} />
    ),
    [handleActionsInteractionChange, refetch],
  );

  return (
    <ClusterNodesSectionWrapper
      title="Data Nodes"
      titleCount={totalDataNodes}
      onTitleCountClick={onSelectSegment ?? null}
      headerLeftSection={(isLoading || isLoadingLayout) && <Spinner />}
      collapsible={collapsible}>
      <EntityDataTable<ClusterDataNode>
        entities={dataNodes}
        columnsOrder={columnsOrder}
        columnPreferences={columnPreferences}
        defaultDisplayedColumns={defaultDisplayedColumns}
        onColumnPreferencesChange={handleColumnPreferencesChange}
        onSortChange={handleSortChange}
        activeSort={searchParams.sort}
        entityAttributesAreCamelCase
        entityActions={renderActions}
        columnSchemas={columnSchemas}
        columnRenderers={columnRenderers}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default DataNodesExpandable;
