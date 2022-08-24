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
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import type { PaginatedUsers } from 'stores/users/UsersStore';
import UsersDomain from 'domainActions/users/UsersDomain';
import { UsersActions } from 'stores/users/UsersStore';
import useCurrentUser from 'hooks/useCurrentUser';
import { DataTable, Spinner, PaginatedList, EmptyResult } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import type UserOverview from 'logic/users/UserOverview';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import UserOverviewItem from './UserOverviewItem';
import UsersFilter from './UsersFilter';
import ClientAddressHead from './ClientAddressHead';
import SystemAdministrator from './SystemAdministratorOverview';

const TABLE_HEADERS = ['', 'Full name', 'Username', 'E-Mail Address', 'Client Address', 'Enabled', 'Role', 'Actions'];

const Container = styled.div`
  .data-table {
    overflow-x: visible;
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _headerCellFormatter = (header) => {
  switch (header.toLowerCase()) {
    case 'client address':
      return <ClientAddressHead title={header} />;
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const _loadUsers = (pagination, setLoading, setPaginatedUsers) => {
  setLoading(true);

  UsersDomain.loadUsersPaginated(pagination).then((paginatedUsers) => {
    setPaginatedUsers(paginatedUsers);
    setLoading(false);
  });
};

const _updateListOnUserDelete = (callback: () => void) => UsersActions.delete.completed.listen(() => callback());
const _updateListOnUserSetStatus = (pagination, setLoading, setPaginatedUsers) => UsersActions.setStatus.completed.listen(() => _loadUsers(pagination, setLoading, setPaginatedUsers));

const buildUsersOverviewItem = (currentUser: any) => (user: UserOverview) => {
  const { id: userId } = user;

  return <UserOverviewItem user={user} isActive={(currentUser?.id === userId)} />;
};

const UsersOverview = () => {
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const currentUser = useCurrentUser();
  const [paginatedUsers, setPaginatedUsers] = useState<PaginatedUsers | undefined>();
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState('');
  const { list: users, adminUser, pagination: { total = 0 } = {} } = paginatedUsers || {};

  useEffect(() => _loadUsers({ page, perPage, query }, setLoading, setPaginatedUsers), [page, perPage, query]);
  useEffect(() => _updateListOnUserDelete(resetPage), [resetPage]);
  useEffect(() => _updateListOnUserSetStatus({ page, perPage, query }, setLoading, setPaginatedUsers), [page, perPage, query]);

  if (!users) {
    return <Spinner />;
  }

  const handleSearch = (newQuery: string) => {
    resetPage();
    setQuery(newQuery);
  };

  const searchFilter = <UsersFilter onSearch={handleSearch} />;

  return (
    <Container>
      {adminUser && (
        <SystemAdministrator adminUser={adminUser}
                             dataRowFormatter={buildUsersOverviewItem(currentUser)}
                             headerCellFormatter={_headerCellFormatter}
                             headers={TABLE_HEADERS} />
      )}
      <Row className="content">
        <Col xs={12}>
          <Header>
            <h2>Users</h2>
            {loading && <LoadingSpinner text="" delay={0} />}
          </Header>
          <p className="description">
            Found {total} registered users on the system.
          </p>
          <StyledPaginatedList totalItems={total}>
            <DataTable id="users-overview"
                       className="table-hover"
                       rowClassName="no-bm"
                       headers={TABLE_HEADERS}
                       headerCellFormatter={_headerCellFormatter}
                       sortByKey="fullName"
                       noDataText={<EmptyResult>No users have been found.</EmptyResult>}
                       rows={users.toJS()}
                       customFilter={searchFilter}
                       dataRowFormatter={buildUsersOverviewItem(currentUser)}
                       filterKeys={[]}
                       filterLabel="Filter Users" />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default UsersOverview;
