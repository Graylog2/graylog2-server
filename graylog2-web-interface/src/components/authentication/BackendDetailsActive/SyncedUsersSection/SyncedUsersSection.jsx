// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';
import * as Immutable from 'immutable';

import Role from 'logic/roles/Role';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';
import { DataTable, PaginatedList, Spinner, EmptyResult } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';

import SyncedUsersOverviewItem from './SyncedUsersOverviewItem';
import ServiceUsersFilter from './SyncedUsersFilter';

const DEFAULT_PAGINATION = {
  count: undefined,
  page: 1,
  perPage: 10,
  query: '',
  total: undefined,
};

const TABLE_HEADERS = ['Username', 'Full Name', 'Roles', 'Actions'];

const _onPageChange = (loadUsers, setLoading) => (page, perPage) => {
  setLoading(true);

  return loadUsers(page, perPage).then(() => setLoading(false));
};

const _headerCellFormatter = (header) => {
  switch (header.toLocaleLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

type Props = {
  roles: Immutable.List<Role>,
};

const SyncedUsersSection = ({ roles }: Props) => {
  const [loading, setLoading] = useState(false);
  const [paginatedUsers, setPaginatedUsers] = useState({ adminUser: undefined, list: undefined, pagination: DEFAULT_PAGINATION });
  const { list: users, pagination: { page, perPage, query, total } } = paginatedUsers;

  const _userOverviewItem = (user) => <SyncedUsersOverviewItem user={user} roles={roles} />;

  const _loadUsers = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return AuthenticationDomain.loadUsersPaginated(newPage, newPerPage, newQuery).then(setPaginatedUsers);
  };

  const _handleSearch = (newQuery) => _loadUsers(DEFAULT_PAGINATION.page, undefined, newQuery);
  const _refreshList = () => _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);

  useEffect(() => {
    _loadUsers(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query);

    const unlistenDisableUser = AuthenticationActions.disableUser.completed.listen(_refreshList);
    const unlistenEnableUser = AuthenticationActions.enableUser.completed.listen(_refreshList);

    return () => {
      unlistenDisableUser();
      unlistenEnableUser();
    };
  }, []);

  if (!users) {
    return <Spinner />;
  }

  return (
    <SectionComponent title="Synchronized Users" showLoading={loading}>
      <p className="description">
        Found {total} synchronized users.
      </p>
      <PaginatedList activePage={page} totalItems={total} onChange={_onPageChange(_loadUsers, setLoading)}>
        <DataTable className="table-hover"
                   customFilter={<ServiceUsersFilter onSearch={_handleSearch} onReset={() => _handleSearch('')} />}
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
