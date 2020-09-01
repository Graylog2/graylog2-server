// @flow strict
import * as React from 'react';
import { useState } from 'react';
import styled from 'styled-components';

import type { AdditionalQueries } from 'util/PaginationURL';
import mockedPermissions from 'logic/permissions/mocked';
import type { PaginatedEnititySharesType } from 'actions/permissions/EntityShareActions';
import { DataTable, PaginatedList } from 'components/common';

import SharedEntitiesFilter from './SharedEntitiesFilter';
import SharedEntitiesOverviewItem from './SharedEntitiesOverviewItem';

const TABLE_HEADERS = ['Entiy Name', 'Entity Type', 'Owner', 'Capability'];

type Props = {
  entityType: string,
  paginatedEntityShares: PaginatedEnititySharesType,
  searchPaginated: (newPage: number, newPerPage: number, newQuery: string, additonalQueries?: AdditionalQueries) => Promise<?PaginatedEnititySharesType>,
  setLoading: (loading: boolean) => void,
};

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _onPageChange = (fetchSharedEntities, setLoading) => (page, perPage) => {
  setLoading(true);

  return fetchSharedEntities(page, perPage).then(() => setLoading(false));
};

const _sharedEntityOverviewItem = (sharedEntity, context) => {
  const capability = context?.granteeCapabilities?.[sharedEntity.id];
  const capabilityTitle = mockedPermissions.availableCapabilities[capability];

  return <SharedEntitiesOverviewItem sharedEntity={sharedEntity} capabilityTitle={capabilityTitle} />;
};

const SharedEntitiesOverview = ({ paginatedEntityShares: initialPaginatedEntityShares, entityType, searchPaginated, setLoading }: Props) => {
  const [paginatedEntityShares, setPaginatedEntityShares] = useState<PaginatedEnititySharesType>(initialPaginatedEntityShares);
  const { list, pagination: { page, perPage, total, query, additionalQueries }, context } = paginatedEntityShares;

  const _loadSharedEntities = (newPage = page, newPerPage = perPage, newQuery = query, newAdditonalQueries = additionalQueries) => {
    return searchPaginated(newPage, newPerPage, newQuery, newAdditonalQueries)
      .then((newPaginatedEntityShares) => newPaginatedEntityShares && setPaginatedEntityShares(newPaginatedEntityShares));
  };

  const _handleSearch = (newQuery: string) => _loadSharedEntities(initialPaginatedEntityShares.pagination.page, undefined, newQuery);
  const _handleFilter = (param: string, value: string) => _loadSharedEntities(initialPaginatedEntityShares.pagination.page, undefined, query, { ...additionalQueries, [param]: value });

  return (
    <>
      <p className="description">
        Found {total} entities which are shared with the {entityType}.
      </p>
      <StyledPaginatedList activePage={page}
                           onChange={_onPageChange(_loadSharedEntities, setLoading)}
                           totalItems={total}>
        <DataTable className="table-hover"
                   customFilter={(
                     <SharedEntitiesFilter onSearch={_handleSearch}
                                           onReset={() => _handleSearch('')}
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
