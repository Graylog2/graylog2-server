// @flow strict
import * as React from 'react';
import { useState } from 'react';
import * as Immutable from 'immutable';
import styled from 'styled-components';

import { EntityShareActions } from 'stores/permissions/EntityShareStore';
import type { PaginatedUserSharesType } from 'stores/permissions/EntityShareStore';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import User from 'logic/users/User';

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

const _onPageChange = (query, setLoading) => (page, perPage) => {
  setLoading(true);

  return EntityShareActions.searchPaginatedUserShares(page, perPage, query).then(() => setLoading(false));
};

const SharedEntitiesSection = ({ paginatedUserShares, username }: Props) => {
  const { list, pagination } = paginatedUserShares || { list: Immutable.List(), pagination: { total: 0 } };
  const [currentSearchQuery, setCurrentSearchQuery] = useState<string>(paginatedUserShares?.pagination?.query || '');
  const [loading, setLoading] = useState(false);
  const _fetchSharedEntities = (page, perPage, query, additonalQueries) => EntityShareActions.searchPaginatedUserShares(username, page, perPage, query, additonalQueries);
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

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>
      {!paginatedUserShares && <Spinner />}
      {paginatedUserShares && (
        <>
          <p className="description">
            Found {pagination.total} entities which got shared with the user.
          </p>
          <StyledPaginatedList onChange={_onPageChange(pagination.query, setLoading)} totalItems={pagination.total} activePage={pagination.page}>
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
