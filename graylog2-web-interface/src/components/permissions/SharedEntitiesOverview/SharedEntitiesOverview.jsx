// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import styled from 'styled-components';

import type { Pagination } from 'stores/PaginationTypes';
import mockedPermissions from 'logic/permissions/mocked';
import type { PaginatedEntityShares } from 'actions/permissions/EntityShareActions';
import { DataTable, PaginatedList } from 'components/common';

import SharedEntitiesFilter from './SharedEntitiesFilter';
import SharedEntitiesOverviewItem from './SharedEntitiesOverviewItem';

const TABLE_HEADERS = ['Entiy Name', 'Entity Type', 'Owner', 'Capability'];

type Props = {
  entityType: string,
  paginatedEntityShares: PaginatedEntityShares,
  searchPaginated: (pagination: Pagination) => Promise<PaginatedEntityShares>,
  setLoading: (loading: boolean) => void,
};

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _sharedEntityOverviewItem = (sharedEntity, { granteeCapabilities } = {}) => {
  const capability = granteeCapabilities?.[sharedEntity.id];
  const capabilityTitle = mockedPermissions.availableCapabilities[capability];

  return <SharedEntitiesOverviewItem sharedEntity={sharedEntity} capabilityTitle={capabilityTitle} />;
};

const _loadSharedEntites = (pagination, initialPaginatedEntityShares, searchPaginated, setPaginatedEntityShares, setLoading) => {
  setLoading(true);

  searchPaginated(pagination).then((paginatedEntityShares) => {
    setLoading(false);
    setPaginatedEntityShares(paginatedEntityShares);
  });
};

const SharedEntitiesOverview = ({ paginatedEntityShares: initialPaginatedEntityShares, entityType, searchPaginated, setLoading }: Props) => {
  const [paginatedEntityShares, setPaginatedEntityShares] = useState<PaginatedEntityShares>(initialPaginatedEntityShares);
  const { list, context, total } = paginatedEntityShares;
  const [pagination, setPagination] = useState(initialPaginatedEntityShares.pagination);
  const { page, query, additionalQueries } = pagination;

  useEffect(() => _loadSharedEntites(pagination, initialPaginatedEntityShares, searchPaginated, setPaginatedEntityShares, setLoading), [pagination, initialPaginatedEntityShares, searchPaginated, setLoading]);

  const _handleSearch = (newQuery: string) => setPagination({ ...pagination, query: newQuery });
  const _handleFilter = (param: string, value: string) => setPagination({ ...pagination, query, additionalQueries: { ...additionalQueries, [param]: value } });

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
