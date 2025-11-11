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

import { EntityDataTable, Spinner } from 'components/common';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';
import type { SearchParams, Sort } from 'stores/PaginationTypes';

import ClusterNodesSectionWrapper from './ClusterNodesSectionWrapper';
import {
  DEFAULT_VISIBLE_COLUMNS,
  createColumnDefinitions,
  createColumnRenderers,
} from './DataNodesColumnConfiguration';
import useClusterDataNodes, { type ClusterDataNode } from './useClusterDataNodes';

const DEFAULT_SEARCH_PARAMS: SearchParams = {
  query: '',
  page: 1,
  pageSize: 0,
  sort: { attributeId: 'hostname', direction: 'asc' },
};

type Props = {
  collapsible?: boolean;
  searchQuery?: string;
  onSelectSegment?: () => void;
};

const DataNodesExpandable = ({ collapsible = true, searchQuery: _searchQuery = '', onSelectSegment = undefined }: Props) => {
  const columnsOrder = useMemo<Array<string>>(() => [...DEFAULT_VISIBLE_COLUMNS], []);
  const [visibleColumns, setVisibleColumns] = useState<Array<string>>([...DEFAULT_VISIBLE_COLUMNS]);
  const [searchParams, setSearchParams] = useState<SearchParams>(DEFAULT_SEARCH_PARAMS);
  const {
    data: dataNodesResponse,
    refetch,
    isInitialLoading,
  } = useClusterDataNodes(searchParams);

  const columnDefinitions = useMemo(() => createColumnDefinitions(), []);
  const columnRenderers = useMemo(() => createColumnRenderers(), []);

  const dataNodes = dataNodesResponse?.list ?? [];
  const totalDataNodes = dataNodesResponse?.pagination?.total ?? dataNodes.length;

  const handleColumnsChange = useCallback((newColumns: Array<string>) => {
    if (!newColumns.length) {
      return;
    }

    setVisibleColumns(newColumns);
  }, []);

  const handleSortChange = useCallback((newSort: Sort) => {
    setSearchParams((prev) => ({
      ...prev,
      sort: newSort,
    }));
  }, []);
  const renderActions = useCallback(
    (entity: ClusterDataNode) => <DataNodeActions dataNode={entity} refetch={refetch} />,
    [refetch],
  );

  return (
    <ClusterNodesSectionWrapper
      title="Data Nodes"
      titleCount={totalDataNodes}
      onTitleCountClick={onSelectSegment ?? null}
      titleCountAriaLabel="Show Data Nodes segment"
      headerLeftSection={isInitialLoading && <Spinner />}
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
        columnDefinitions={columnDefinitions}
        columnRenderers={columnRenderers}
      />
    </ClusterNodesSectionWrapper>
  );
};

export default DataNodesExpandable;
