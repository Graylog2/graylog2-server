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
import styled from 'styled-components';

import { EntityDataTable, NoSearchResult, Spinner } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';

import { createColumnDefinitions, createColumnRenderers } from './DataNodesColumnConfiguration';
import useClusterDataNodesTableLayout from './useClusterDataNodesTableLayout';
import useClusterDataNodes, { type ClusterDataNode } from './useClusterDataNodes';

import ClusterNodesSectionWrapper from '../shared-components/ClusterNodesSectionWrapper';

const EmptyState = styled(NoSearchResult)`
  margin-top: ${({ theme }) => theme.spacings.xl};
`;

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectNodeType?: () => void;
  pageSizeLimit?: number;
  refetchInterval?: number | false;
};

const DataNodesExpandable = ({
  collapsible = true,
  searchQuery = '',
  onSelectNodeType = undefined,
  pageSizeLimit = undefined,
  refetchInterval = undefined,
}: Props) => {
  const {
    defaultDisplayedColumns,
    defaultColumnOrder,
    layoutPreferences,
    searchParams,
    isLoadingLayout,
    handleLayoutPreferencesChange,
    handleSortChange,
    resetLayoutPreferences,
  } = useClusterDataNodesTableLayout(searchQuery, pageSizeLimit);
  const {
    nodes: dataNodes,
    total: totalDataNodes,
    refetch,
    isLoading,
    setPollingEnabled,
  } = useClusterDataNodes(searchParams, { refetchInterval });

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
      onTitleCountClick={onSelectNodeType ?? null}
      headerLeftSection={(isLoading || isLoadingLayout) && <Spinner />}
      collapsible={collapsible}>
      {(() => {
        if (isLoading || isLoadingLayout) {
          return <Spinner />;
        }

        if (dataNodes.length === 0) {
          return <EmptyState>No Data Nodes found.</EmptyState>;
        }

        return (
          <EntityDataTable<ClusterDataNode>
            entities={dataNodes}
            defaultDisplayedColumns={defaultDisplayedColumns}
            defaultColumnOrder={defaultColumnOrder}
            layoutPreferences={layoutPreferences}
            onResetLayoutPreferences={resetLayoutPreferences}
            onLayoutPreferencesChange={handleLayoutPreferencesChange}
            onSortChange={handleSortChange}
            activeSort={searchParams.sort}
            entityAttributesAreCamelCase
            entityActions={renderActions}
            columnSchemas={columnSchemas}
            columnRenderers={columnRenderers}
          />
        );
      })()}
    </ClusterNodesSectionWrapper>
  );
};

export default DataNodesExpandable;
