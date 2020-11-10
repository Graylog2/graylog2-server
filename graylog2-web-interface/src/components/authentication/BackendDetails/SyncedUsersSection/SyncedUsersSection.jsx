// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';
import type { PaginatedUsers } from 'actions/users/UsersActions';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';
import { DataTable, PaginatedList, Spinner, EmptyResult } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

import SyncedUsersOverviewItem from './SyncedUsersOverviewItem';
import SyncedUsersFilter from './SyncedUsersFilter';

const TABLE_HEADERS = ['Username', 'Full Name', 'Roles', 'Actions'];
const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

const _headerCellFormatter = (header) => {
  switch (header.toLocaleLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const _loadSyncedTeams = (authBackendId, pagination, setLoading, setPaginatedUsers) => {
  setLoading(true);

  AuthenticationDomain.loadUsersPaginated(authBackendId, pagination).then((paginatedUsers) => {
    setPaginatedUsers(paginatedUsers);
    setLoading(false);
  });
};

type Props = {
  roles: Immutable.List<Role>,
  authenticationBackend: AuthenticationBackend,
};

const SyncedUsersSection = ({ roles, authenticationBackend }: Props) => {
  const [loading, setLoading] = useState(false);
  const [paginatedUsers, setPaginatedUsers] = useState<?PaginatedUsers>();
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: users } = paginatedUsers || {};
  const { page, perPage, query } = pagination;

  useEffect(() => _loadSyncedTeams(authenticationBackend.id, pagination, setLoading, setPaginatedUsers), [authenticationBackend.id, pagination]);

  if (!paginatedUsers) {
    return <Spinner />;
  }

  const _userOverviewItem = (user) => <SyncedUsersOverviewItem user={user} roles={roles} />;

  return (
    <SectionComponent title="Synchronized Users" showLoading={loading}>
      <p className="description">
        Found {paginatedUsers.pagination.total} synchronized users.
      </p>
      <PaginatedList activePage={page} totalItems={paginatedUsers.pagination.total} onChange={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })}>
        <DataTable className="table-hover"
                   customFilter={<SyncedUsersFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })} />}
                   dataRowFormatter={_userOverviewItem}
                   filterKeys={[]}
                   filterLabel="Filter Users"
                   headerCellFormatter={_headerCellFormatter}
                   headers={TABLE_HEADERS}
                   id="synced-users-overview"
                   noDataText={<EmptyResult>No synchronized users have been found.</EmptyResult>}
                   rowClassName="no-bm"
                   rows={users.toJS()}
                   sortByKey="username" />
      </PaginatedList>
    </SectionComponent>
  );
};

export default SyncedUsersSection;
