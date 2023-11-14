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
import { useQuery } from '@tanstack/react-query';
import { useQueryParam, StringParam } from 'use-query-params';

import type { DataNode, DataNodeStatus } from 'preflight/types';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import { PaginatedList, SearchForm, NoSearchResult } from 'components/common';
import MenuItem from 'components/bootstrap/MenuItem';
import Spinner from 'components/common/Spinner';
import QueryHelper from 'components/common/QueryHelper';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import EntityDataTable from 'components/common/EntityDataTable';
import useUpdateUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUpdateUserLayoutPreferences';
import useTableLayout from 'components/common/EntityDataTable/hooks/useTableLayout';
import { defaultCompare } from 'logic/DefaultCompare';
import type { Sort } from 'stores/PaginationTypes';
import useTableEventHandlers from 'hooks/useTableEventHandlers';
import { fetchPeriodically } from 'logic/rest/FetchProvider';

import BulkActionsDropdown from '../common/EntityDataTable/BulkActionsDropdown';
import { MORE_ACTIONS_HOVER_TITLE, MORE_ACTIONS_TITLE } from '../common/EntityDataTable/Constants';
import OverlayDropdownButton from '../common/OverlayDropdownButton';

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

export const fetchDataNodes = () => fetchPeriodically<Array<DataNode>>('GET', qualifyUrl('/certrenewal'));

const useDataNodes = () => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['data-nodes', 'overview'],
    queryFn: fetchDataNodes,
    onError: (errorThrown) => {
      UserNotification.error(`Loading data nodes failed with status: ${errorThrown}`,
        'Could not load datanodes');
    },
    keepPreviousData: true,
    refetchInterval: 3000,

  });

  const mockData = [
    {
      id: '1',
      is_leader: false,
      is_master: false,
      last_seen: '2023-11-02T13:20:58',
      cert_valid_until: '2053-11-02T13:20:58',
      error_msg: null,
      hostname: 'datanode1',
      node_id: '3af165ef-87a9-467f-b7db-435f4748eb75',
      short_node_id: '3af165ef',
      status: 'CONNECTED' as DataNodeStatus,
      transport_address: 'http://datanode1:9200',
      type: 'DATANODE',
    },
    {
      id: '2',
      is_leader: false,
      is_master: false,
      last_seen: '2023-11-02T13:20:58',
      cert_valid_until: '2053-11-02T13:20:58',
      error_msg: null,
      hostname: 'datanode2',
      node_id: '9597fd2f-9c44-466b-ae47-e49ba54d3aeb',
      short_node_id: '9597fd2f',
      status: 'CONNECTED' as DataNodeStatus,
      transport_address: 'http://datanode2:9200',
      type: 'DATANODE',
    },
  ];

  return ({
    data: mockData || data,
    isInitialLoading,
  });
};

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
  const bulkSelection: any = {
    // eslint-disable-next-line react/no-unstable-nested-components
    actions: () => (
      <BulkActionsDropdown selectedEntities={['']} setSelectedEntities={() => {}}>
        <MenuItem onSelect={() => {}}>Restart</MenuItem>
        <MenuItem onSelect={() => {}}>Remove</MenuItem>
      </BulkActionsDropdown>
    ),
    onChangeSelection: () => {},
    initialSelection: [],
  };
  const entityActions = () => (
    <OverlayDropdownButton title={MORE_ACTIONS_TITLE}
                           bsSize="xsmall"
                           buttonTitle={MORE_ACTIONS_HOVER_TITLE}
                           disabled={false}
                           dropdownZIndex={1000}>
      <MenuItem onSelect={() => {}}>Edit</MenuItem>
      <MenuItem onSelect={() => {}}>Restart</MenuItem>
      <MenuItem onSelect={() => {}}>Remove</MenuItem>
    </OverlayDropdownButton>
  );
  const columnRenderers = useMemo(() => ({ attributes: {} }), []);
  const columnDefinitions = useMemo(() => ATTRIBUTES, []);

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
                                     bulkSelection={bulkSelection}
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
