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
import React, { useMemo } from 'react';

import { EntityDataTable, Spinner } from 'components/common';
import type { ColumnSchema } from 'components/common/EntityDataTable';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';

import ClusterNodesSectionWrapper from './ClusterNodesSectionWrapper';
import {
  createColumnDefinitions,
  createColumnRenderers,
} from './DataNodesColumnConfiguration';
import useClusterDataNodesTableLayout from './useClusterDataNodesTableLayout';
import useClusterDataNodes, { type ClusterDataNode } from './useClusterDataNodes';

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectSegment?: () => void;
};

const DataNodesExpandable = ({ collapsible = true, searchQuery: _searchQuery = '', onSelectSegment = undefined }: Props) => {
  const {
    columnsOrder,
    visibleColumns,
    searchParams,
    isLoadingLayout,
    handleColumnsChange,
    handleSortChange,
  } = useClusterDataNodesTableLayout();
  const {
    data: dataNodesResponse,
    refetch,
    isInitialLoading,
  } = useClusterDataNodes(searchParams);

  const columnSchemas = useMemo<Array<ColumnSchema>>(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const dataNodes = dataNodesResponse?.list ?? [];
  const totalDataNodes = dataNodesResponse?.pagination?.total ?? dataNodes.length;

  const renderActions = (entity: ClusterDataNode) => <DataNodeActions dataNode={entity} refetch={refetch} />;

  return (
    <ClusterNodesSectionWrapper
      title="Data Nodes"
      titleCount={totalDataNodes}
      onTitleCountClick={onSelectSegment ?? null}
      titleCountAriaLabel="Show Data Nodes segment"
      headerLeftSection={(isInitialLoading || isLoadingLayout) && <Spinner />}
      collapsible={collapsible}>
      <EntityDataTable<ClusterDataNode>
        entities={dataNodes}
        visibleColumns={visibleColumns}
        columnsOrder={columnsOrder}
        onColumnsChange={handleColumnsChange}
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
