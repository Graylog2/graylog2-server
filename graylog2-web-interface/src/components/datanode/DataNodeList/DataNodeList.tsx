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
import React, { useEffect } from 'react';
import styled from 'styled-components';
import { useQueryParam, StringParam } from 'use-query-params';

import type { DataNode, DataNodeStatus } from 'preflight/types';
import { PaginatedList, SearchForm, NoSearchResult, RelativeTime } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import EntityDataTable from 'components/common/EntityDataTable';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import type { Sort } from 'stores/PaginationTypes';
import useTableEventHandlers from 'hooks/useTableEventHandlers';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import DataNodeActions from './DataNodeActions';
import DataNodeStatusCell from './DataNodeStatusCell';

import useDataNodes from '../hooks/useDataNodes';

const ENTITY_TABLE_ID = 'datanodes';
const DEFAULT_LAYOUT = {
  pageSize: 10,
  sort: { attributeId: 'title', direction: 'asc' } as Sort,
  displayedColumns: ['hostname', 'transport_address', 'status', 'is_leader', 'cert_valid_until'],
  columnsOrder: ['hostname', 'transport_address', 'status', 'is_leader', 'cert_valid_until'],
};

const columnDefinitions = [
  { id: 'hostname', title: 'Name', sortable: true, permissions: [] },
  { id: 'transport_address', title: 'Transport address' },
  { id: 'status', title: 'Status', sortable: false },
  { id: 'is_leader', title: 'Is leader', sortable: true },
  { id: 'cert_valid_until', title: 'Certificate valid until', sortable: false },
];

const columnRenderers: ColumnRenderers<DataNode> = {
  attributes: {
    hostname: {
      renderCell: (_hostname: string, dataNode: DataNode) => (
        <Link to={Routes.SYSTEM.DATANODES.SHOW(dataNode.node_id)}>
          {dataNode.hostname}
        </Link>
      ),
    },
    status: {
      renderCell: (_status: DataNodeStatus, dataNode: DataNode) => <DataNodeStatusCell dataNode={dataNode} />,
    },
    is_leader: {
      renderCell: (_is_leader: string, dataNode: DataNode) => (dataNode.is_leader ? 'yes' : 'no'),
    },
    cert_valid_until: {
      renderCell: (_cert_valid_until: string, dataNode: DataNode) => <RelativeTime dateTime={dataNode.cert_valid_until} />,
    },
  },
};

const entityActions = (dataNode: DataNode) => (
  <DataNodeActions dataNode={dataNode} />
);

const SearchContainer = styled.div`
  margin-bottom: 5px;
`;

const DataNodeList = () => {
  const [query, setQuery] = useQueryParam('query', StringParam);
  const { layoutConfig, isInitialLoading: isLoadingLayoutPreferences } = useTableLayout({
    entityTableId: ENTITY_TABLE_ID,
    defaultPageSize: DEFAULT_LAYOUT.pageSize,
    defaultDisplayedAttributes: DEFAULT_LAYOUT.displayedColumns,
    defaultSort: DEFAULT_LAYOUT.sort,
  });
  const paginationQueryParameter = usePaginationQueryParameter(undefined, layoutConfig.pageSize, false);
  const { mutate: updateTableLayout } = useUpdateUserLayoutPreferences(ENTITY_TABLE_ID);
  const { data: { elements, pagination: { total } }, isInitialLoading: isInitialLoadingDataNodes, refetch } = useDataNodes({
    query: query,
    page: paginationQueryParameter.page,
    pageSize: layoutConfig.pageSize,
    sort: layoutConfig.sort,
  }, { enabled: !isLoadingLayoutPreferences });

  useEffect(() => {
    refetch();
  }, [query, paginationQueryParameter.page, layoutConfig.pageSize, layoutConfig.sort, refetch]);

  const {
    onPageSizeChange,
    onSearch,
    onSearchReset,
    onColumnsChange,
    onSortChange,
  } = useTableEventHandlers({
    paginationQueryParameter,
    updateTableLayout,
    setQuery,
  });

  if (isLoadingLayoutPreferences || isInitialLoadingDataNodes) {
    return <Spinner />;
  }

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
                   totalItems={total}>
      <SearchContainer>
        <SearchForm onSearch={onSearch}
                    onReset={onSearchReset}
                    query={query}
                    queryHelpComponent={(
                      <QueryHelper entityName="datanode"
                                   commonFields={['name']}
                                   example={(
                                     <p>
                                       Find entities with a description containing node:<br />
                                       <code>name:node</code><br />
                                     </p>
                                   )} />
                    )} />
      </SearchContainer>
      <div>
        {elements?.length === 0 ? (
          <NoSearchResult>No Data Nodes have been found</NoSearchResult>
        ) : (
          <EntityDataTable<DataNode> data={elements}
                                     visibleColumns={layoutConfig.displayedAttributes}
                                     columnsOrder={DEFAULT_LAYOUT.columnsOrder}
                                     onColumnsChange={onColumnsChange}
                                     onSortChange={onSortChange}
                                     onPageSizeChange={onPageSizeChange}
                                     pageSize={layoutConfig.pageSize}
                                     bulkSelection={{}}
                                     activeSort={layoutConfig.sort}
                                     rowActions={entityActions}
                                     actionsCellWidth={160}
                                     columnRenderers={columnRenderers}
                                     columnDefinitions={columnDefinitions}
                                     entityAttributesAreCamelCase={false} />
        )}
      </div>
    </PaginatedList>
  );
};

export default DataNodeList;
