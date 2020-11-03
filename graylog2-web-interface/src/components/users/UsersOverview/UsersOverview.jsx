// @flow strict
import * as React from 'react';
import { useEffect, useContext, useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { PaginatedUsers } from 'actions/users/UsersActions';
import UsersDomain from 'domainActions/users/UsersDomain';
import { UsersActions } from 'stores/users/UsersStore';
import { type ThemeInterface } from 'theme';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { DataTable, Spinner, PaginatedList, EmptyResult } from 'components/common';
import { Col, Row } from 'components/graylog';

import UserOverviewItem from './UserOverviewItem';
import UsersFilter from './UsersFilter';
import ClientAddressHead from './ClientAddressHead';
import SystemAdministrator from './SystemAdministratorOverview';

const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

const TABLE_HEADERS = ['', 'Full name', 'Username', 'E-Mail Address', 'Client Address', 'Role', 'Actions'];

const Container: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  .data-table {
    overflow-x: visible;
  }
`;

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => `
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

const UsersOverview = () => {
  const currentUser = useContext(CurrentUserContext);
  const [paginatedUsers, setPaginatedUsers] = useState<?PaginatedUsers>();
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: users, adminUser, pagination: { total } = {} } = paginatedUsers || {};
  const { page, query, perPage } = pagination;

  useEffect(() => _loadUsers(pagination, setLoading, setPaginatedUsers), [pagination]);
  useEffect(() => _updateListOnUserDelete(perPage, query, setPagination), [perPage, query]);

  if (!users) {
    return <Spinner />;
  }

  const searchFilter = <UsersFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })} />;
  const _usersOverviewItem = (user) => <UserOverviewItem user={user} isActive={(currentUser?.id === user.id)} />;

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
