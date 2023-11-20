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
import React from 'react';
import { useQueryParam, StringParam } from 'use-query-params';

import type { DataNode } from 'preflight/types';
import { PaginatedList, SearchForm, NoSearchResult } from 'components/common';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import EntityDataTable from 'components/common/EntityDataTable';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import { defaultCompare } from 'logic/DefaultCompare';
import type { Sort } from 'stores/PaginationTypes';
import useTableEventHandlers from 'hooks/useTableEventHandlers';

import DataNodeBulkActions from './DataNodeBulkActions';
import DataNodeActions from './DataNodeActions';

import useDataNodes from '../hooks/useDataNodes';

export const ENTITY_TABLE_ID = 'datanodes';
export const DEFAULT_LAYOUT = {
  pageSize: 20,
  sort: { attributeId: 'title', direction: 'asc' } as Sort,
  displayedColumns: ['hostname', 'transport_address', 'status', 'cert_valid_until', 'last_seen'],
  columnsOrder: ['hostname', 'transport_address', 'status', 'cert_valid_until', 'last_seen'],
};

export const ATTRIBUTES = [
  { id: 'hostname', title: 'Name', sortable: true, permissions: [] },
  { id: 'transport_address', title: 'Transport address' },
  { id: 'status', title: 'Status', sortable: true },
  { id: 'cert_valid_until', title: 'Certificate valid until', sortable: true },
  { id: 'last_seen', title: 'Last seen', sortable: true },
];

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
  const { data: dataNodes, isInitialLoading: isInitialLoadingDataNodes } = useDataNodes();
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

  const elements = dataNodes?.sort((d1, d2) => defaultCompare(d1.cert_valid_until, d2.cert_valid_until));
  const total = elements?.length || 0;
  const bulkActions: any = () => (
    <DataNodeBulkActions selectedDataNodeIds={[]}
                         setSelectedDataNodeIds={() => {}}
                         dataNodes={[]} />
  );
  const entityActions = () => (
    <DataNodeActions />
  );
  const columnRenderers = {
    attributes: {
      status: {
        renderCell: () => null,
        staticWidth: 100,
      },
    },
  };
  const columnDefinitions = [...ATTRIBUTES];

  if (isLoadingLayoutPreferences || isInitialLoadingDataNodes) {
    return <Spinner />;
  }

  return (
    <PaginatedList pageSize={layoutConfig.pageSize}
                   showPageSizeSelect={false}
                   totalItems={total}>
      <div style={{ marginBottom: 5 }}>
        <SearchForm onSearch={onSearch}
                    onReset={onSearchReset}
                    query={query}
                    queryHelpComponent={<QueryHelper entityName="datanode" />} />
      </div>
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
                                     bulkSelection={{
                                       actions: bulkActions,
                                     }}
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
