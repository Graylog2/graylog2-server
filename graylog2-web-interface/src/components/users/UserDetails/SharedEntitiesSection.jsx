// @flow strict
import * as React from 'react';
import { useState } from 'react';
import * as Immutable from 'immutable';

import { EntityShareActions } from 'stores/permissions/EntityShareStore';
import type { PaginatedUserSharesType } from 'stores/permissions/EntityShareStore';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import User from 'logic/users/User';

import SharedEntitiesFilter from './SharedEntitiesFilter';
import SharedEntitiesOverviewItem from './SharedEntitiesOverviewItem';

import SectionComponent from '../SectionComponent';

type Props = {
  username: $PropertyType<User, 'username'>,
  paginatedUserShares: PaginatedUserSharesType,
};

const _onPageChange = (query, setLoading) => (page, perPage) => {
  setLoading(true);

  return EntityShareActions.searchPaginatedUserShares(page, perPage, query).then(() => setLoading(false));
};

const SharedEntitiesSection = ({ paginatedUserShares, username }: Props) => {
  const { list, pagination } = paginatedUserShares || { list: Immutable.List(), pagination: { total: 0 } };
  const [loading, setLoading] = useState(false);
  const headers = ['Entiy Name', 'Entity Type', 'Owner', 'Capability'];
  const _handleSearch = (newQuery, resetLoading) => EntityShareActions.searchPaginatedUserShares(username, 1, pagination.perPage, newQuery).then(resetLoading);
  const _handleSearchReset = () => EntityShareActions.searchPaginatedUserShares(username, 1, pagination.perPage, '');
  const _sharedEntityOverviewItem = (sharedEntity) => <SharedEntitiesOverviewItem sharedEntity={sharedEntity} />;

  return (
    <SectionComponent title="Shared Entities" showLoading={loading}>

      {!paginatedUserShares && <Spinner />}
      {paginatedUserShares && (
        <>
          <p className="description">
            Found {pagination.total} entities which got shared with the user.
          </p>
          <PaginatedList onChange={_onPageChange(pagination.query, setLoading)} totalItems={pagination.total} activePage={pagination.page}>
            <DataTable id="user-shared-entities"
                       className="table-hover"
                       headers={headers}
                       sortByKey="type"
                       rows={list.toJS()}
                       customFilter={<SharedEntitiesFilter onSearch={_handleSearch} onReset={_handleSearchReset} />}
                       dataRowFormatter={_sharedEntityOverviewItem}
                       filterKeys={[]} />
          </PaginatedList>
        </>
      )}
    </SectionComponent>
  );
};

export default SharedEntitiesSection;
