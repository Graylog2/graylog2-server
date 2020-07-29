// @flow strict
import * as React from 'react';
import { useState } from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { EntityShareActions, type UserSharesPaginationType } from 'stores/permissions/EntityShareStore';
import type { PaginatedUserSharesType } from 'stores/permissions/EntityShareStore';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import User from 'logic/users/User';
import type { UserSharedEntities } from 'logic/permissions/types';

import SharedEntitiesFilter from './SharedEntitiesFilter';
import SharedEntitiesOverviewItem from './SharedEntitiesOverviewItem';

import SectionComponent from '../SectionComponent';

const TABLE_HEADERS = ['Entiy Name', 'Entity Type', 'Owner', 'Capability'];

type Props = {
  username: $PropertyType<User, 'username'>,
  paginatedUserShares: PaginatedUserSharesType,
};

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _onPageChange = (pagination, fetchSharedEntities, setLoading) => (page, perPage) => {
  setLoading(true);

  return fetchSharedEntities(page, perPage, pagination.query, pagination.additionalQueries).then(() => setLoading(false));
};

const SharedEntitiesSection = ({ paginatedUserShares: initialPaginatedUserShares, username }: Props) => {
  const [currentSearchQuery, setCurrentSearchQuery] = useState<string>(initialPaginatedUserShares?.pagination?.query || '');
  const [paginatedUserShares, setPaginatedUserShares] = useState<PaginatedUserSharesType>(initialPaginatedUserShares || { list: Immutable.List(), pagination: { total: 0 } });
  const { list, pagination } = paginatedUserShares;
  const [loading, setLoading] = useState(false);

  const _fetchSharedEntities = (newPage, newPerPage, newQuery, additonalQueries) => {
    return EntityShareActions.searchPaginatedUserShares(username, newPage, newPerPage, newQuery, additonalQueries).then((newPaginatedUserShares) => {
      setPaginatedUserShares(newPaginatedUserShares);
    });
  };

  const _sharedEntityOverviewItem = (sharedEntity) => <SharedEntitiesOverviewItem sharedEntity={sharedEntity} />;

  const _handleSearch = (newQuery: string, resetLoading: () => void) => {
    setCurrentSearchQuery(newQuery || '');

    return _fetchSharedEntities(1, pagination.perPage, newQuery).then(resetLoading);
  };

  const _handleSearchReset = () => {
    setCurrentSearchQuery('');

    return _fetchSharedEntities(1, pagination.perPage, '');
  };

  const _handleFilter = (param: string, value: string) => _fetchSharedEntities(1, pagination.perPage, currentSearchQuery, { [param]: value });
  console.log(paginatedUserShares);

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      {!paginatedUserShares && <Spinner />}
      {paginatedUserShares && (
        <>
          <p className="description">
            Found {pagination.total} entities which got shared with the user.
          </p>
          <StyledPaginatedList onChange={_onPageChange(pagination, _fetchSharedEntities, setLoading)} totalItems={pagination.total} activePage={pagination.page}>
            <DataTable id="user-shared-entities"
                       rowClassName="no-bm"
                       className="table-hover"
                       headers={TABLE_HEADERS}
                       sortByKey="type"
                       rows={list.toJS()}
                       customFilter={<SharedEntitiesFilter onSearch={_handleSearch} onReset={_handleSearchReset} onFilter={_handleFilter} />}
                       dataRowFormatter={_sharedEntityOverviewItem}
                       filterKeys={[]} />
          </StyledPaginatedList>
        </>
      )}
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
