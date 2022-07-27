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
import type * as Immutable from 'immutable';

import type Role from 'logic/roles/Role';
import type { PaginatedUsers } from 'stores/users/UsersStore';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import { DataTable, PaginatedList, Spinner, EmptyResult } from 'components/common';
import SectionComponent from 'components/common/Section/SectionComponent';
import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import SyncedUsersOverviewItem from './SyncedUsersOverviewItem';
import SyncedUsersFilter from './SyncedUsersFilter';

const TABLE_HEADERS = ['Username', 'Full Name', 'Roles', 'Actions'];

const _headerCellFormatter = (header) => {
  switch (header.toLowerCase()) {
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

const _userOverviewItem = (user, roles) => <SyncedUsersOverviewItem user={user} roles={roles} />;

const SyncedUsersSection = ({ roles, authenticationBackend }: Props) => {
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const [loading, setLoading] = useState(false);
  const [paginatedUsers, setPaginatedUsers] = useState<PaginatedUsers | undefined>();
  const [query, setQuery] = useState('');
  const { list: users } = paginatedUsers || {};

  useEffect(() => _loadSyncedTeams(authenticationBackend.id, { query, page, perPage }, setLoading, setPaginatedUsers), [authenticationBackend.id, query, page, perPage]);

  const onSearch = (newQuery) => {
    resetPage();
    setQuery(newQuery);
  };

  if (!paginatedUsers) {
    return <Spinner />;
  }

  return (
    <SectionComponent title="Synchronized Users" showLoading={loading}>
      <p className="description">
        Found {paginatedUsers.pagination.total} synchronized users.
      </p>
      <PaginatedList totalItems={paginatedUsers.pagination.total}>
        <DataTable className="table-hover"
                   customFilter={<SyncedUsersFilter onSearch={onSearch} />}
                   dataRowFormatter={(user) => _userOverviewItem(user, roles)}
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
