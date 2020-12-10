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
import { useEffect, useContext, useState } from 'react';
import styled, { css } from 'styled-components';

import type { PaginatedUsers } from 'actions/users/UsersActions';
import UsersDomain from 'domainActions/users/UsersDomain';
import { UsersActions } from 'stores/users/UsersStore';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { DataTable, Spinner, PaginatedList, EmptyResult } from 'components/common';
import { Col, Row } from 'components/graylog';
import UserOverview from 'logic/users/UserOverview';
import { UserJSON } from 'logic/users/User';

import UserOverviewItem from './UserOverviewItem';
import UsersFilter from './UsersFilter';
import ClientAddressHead from './ClientAddressHead';
import SystemAdministrator from './SystemAdministratorOverview';

const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

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
  switch (header.toLocaleLowerCase()) {
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

const _updateListOnUserDelete = (perPage, query, setPagination) => UsersActions.delete.completed.listen(() => setPagination({ page: DEFAULT_PAGINATION.page, perPage, query }));
const _updateListOnUserSetStatus = (pagination, setLoading, setPaginatedUsers) => UsersActions.setStatus.completed.listen(() => _loadUsers(pagination, setLoading, setPaginatedUsers));

const UsersOverview = () => {
  const currentUser = useContext<UserJSON | undefined>(CurrentUserContext);
  const [paginatedUsers, setPaginatedUsers] = useState<PaginatedUsers | undefined>();
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: users, adminUser, pagination: { total = 0 } = {} } = paginatedUsers || {};
  const { page, query, perPage } = pagination;

  useEffect(() => _loadUsers(pagination, setLoading, setPaginatedUsers), [pagination]);
  useEffect(() => _updateListOnUserDelete(perPage, query, setPagination), [perPage, query]);
  useEffect(() => _updateListOnUserSetStatus(pagination, setLoading, setPaginatedUsers), [pagination]);

  if (!users) {
    return <Spinner />;
  }

  const searchFilter = <UsersFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })} />;
  const _usersOverviewItem = (user: UserOverview) => <UserOverviewItem user={user} isActive={(currentUser?.id === user.id)} />;

  return (
    <Container>
      {adminUser && (
        <SystemAdministrator adminUser={adminUser}
                             dataRowFormatter={_usersOverviewItem}
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
          <StyledPaginatedList activePage={page}
                               totalItems={total}
                               onChange={(newPage, newPerPage) => setPagination({ ...pagination, page: newPage, perPage: newPerPage })}>
            <DataTable id="users-overview"
                       className="table-hover"
                       rowClassName="no-bm"
                       headers={TABLE_HEADERS}
                       headerCellFormatter={_headerCellFormatter}
                       sortByKey="fullName"
                       noDataText={<EmptyResult>No users have been found.</EmptyResult>}
                       rows={users.toJS()}
                       customFilter={searchFilter}
                       dataRowFormatter={_usersOverviewItem}
                       filterKeys={[]}
                       filterLabel="Filter Users" />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default UsersOverview;
