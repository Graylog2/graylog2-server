// @flow strict
import * as React from 'react';
import { useState, useEffect } from 'react';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';
import SectionComponent from 'components/common/Section/SectionComponent';
import { DataTable, PaginatedList, Spinner } from 'components/common';

import SyncedUsersOverviewItem from './SyncedUsersOverviewItem';
import ServiceUsersFilter from './SyncedUsersFilter';

const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
  page: 1,
  perPage: 10,
  query: '',
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

const SyncedUsersSection = () => {
  const [paginatedUsers, setPaginatedUsers] = useState({ adminUser: undefined, list: undefined, pagination: DEFAULT_PAGINATION });
  const { list: users, pagination: { page, perPage, query, total } } = paginatedUsers;
  const [loading, setLoading] = useState(false);
  const _userOverviewItem = (user) => <SyncedUsersOverviewItem user={user} />;

  const _loadUsers = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return AuthenticationDomain.loadUsersPaginated(newPage, newPerPage, newQuery)
      .then((newPaginatedUsers) => newPaginatedUsers && setPaginatedUsers(newPaginatedUsers));
  };

  const _handleSearch = (newQuery) => _loadUsers(DEFAULT_PAGINATION.page, undefined, newQuery);

  useEffect(() => {
    _loadUsers(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query);

    const unlistenDisableUser = AuthenticationActions.disableUser.completed.listen(() => {
      _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenEnableUser = AuthenticationActions.enableUser.completed.listen(() => {
      _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

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
      <PaginatedList onChange={_onPageChange(_loadUsers, setLoading)} totalItems={total} activePage={page}>
        <DataTable id="synced-users-overview"
                   className="table-hover"
                   rowClassName="no-bm"
                   headers={TABLE_HEADERS}
                   headerCellFormatter={_headerCellFormatter}
                   sortByKey="username"
                   rows={users.toJS()}
                   customFilter={<ServiceUsersFilter onSearch={_handleSearch} onReset={() => _handleSearch('')} />}
                   dataRowFormatter={_userOverviewItem}
                   filterKeys={[]}
                   filterLabel="Filter Users" />
      </PaginatedList>
    </SectionComponent>
  );
};

export default SyncedUsersSection;
