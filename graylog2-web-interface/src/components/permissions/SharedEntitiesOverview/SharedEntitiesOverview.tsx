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
import * as React from 'react';
import { useState, useEffect } from 'react';
import styled from 'styled-components';

import type { Pagination } from 'stores/PaginationTypes';
import mockedPermissions from 'logic/permissions/mocked';
import type { PaginatedEntityShares } from 'actions/permissions/EntityShareActions';
import { DataTable, PaginatedList, Spinner, EmptyResult } from 'components/common';

import SharedEntitiesFilter from './SharedEntitiesFilter';
import SharedEntitiesOverviewItem from './SharedEntitiesOverviewItem';

const TABLE_HEADERS = ['Entity Name', 'Entity Type', 'Owner', 'Capability'];
const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

type Props = {
  entityType: string,
  searchPaginated: (pagination: Pagination) => Promise<PaginatedEntityShares>,
  setLoading: (loading: boolean) => void,
};

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _sharedEntityOverviewItem = (sharedEntity, { granteeCapabilities } = { granteeCapabilities: undefined }) => {
  const capability = granteeCapabilities?.[sharedEntity.id];
  const capabilityTitle = mockedPermissions.availableCapabilities[capability];

  return <SharedEntitiesOverviewItem sharedEntity={sharedEntity} capabilityTitle={capabilityTitle} />;
};

const _loadSharedEntities = (pagination, searchPaginated, setPaginatedEntityShares, setLoading) => {
  setLoading(true);

  searchPaginated(pagination).then((paginatedEntityShares) => {
    setLoading(false);
    setPaginatedEntityShares(paginatedEntityShares);
  });
};

const SharedEntitiesOverview = ({ entityType, searchPaginated, setLoading }: Props) => {
  const [paginatedEntityShares, setPaginatedEntityShares] = useState<PaginatedEntityShares | undefined>();
  const [pagination, setPagination] = useState<Pagination>(DEFAULT_PAGINATION);
  const { list, context, pagination: { total } = { total: 0 } } = paginatedEntityShares || {};
  const { page, query, additionalQueries } = pagination;

  useEffect(() => _loadSharedEntities(pagination, searchPaginated, setPaginatedEntityShares, setLoading), [pagination, searchPaginated, setLoading]);

  const _handleSearch = (newQuery: string) => setPagination({ ...pagination, query: newQuery });
  const _handleFilter = (param: string, value: string) => setPagination({ ...pagination, query, additionalQueries: { ...additionalQueries, [param]: value } });

  if (!paginatedEntityShares) {
    return <Spinner />;
  }

  return (
    <>
      <p className="description">
        Found {total} entities which are shared with the {entityType}.
      </p>
      <StyledPaginatedList activePage={page}
                           totalItems={total}
                           onChange={(newPage, newPerPage) => setPagination({ ...pagination, page: newPage, perPage: newPerPage })}>
        <DataTable className="table-hover"
                   customFilter={(
                     <SharedEntitiesFilter onSearch={_handleSearch}
                                           onFilter={_handleFilter} />
                   )}
                   dataRowFormatter={(sharedEntity) => _sharedEntityOverviewItem(sharedEntity, context)}
                   filterKeys={[]}
                   noDataText={<EmptyResult>No shared entities have been found.</EmptyResult>}
                   headers={TABLE_HEADERS}
                   id="shared-entities"
                   rowClassName="no-bm"
                   rows={list.toJS()}
                   sortByKey="type" />
      </StyledPaginatedList>
    </>
  );
};

export default SharedEntitiesOverview;
