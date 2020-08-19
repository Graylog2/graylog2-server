// @flow strict
import * as React from 'react';
import { useEffect, useContext, useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import UsersDomain from 'domainActions/users/UsersDomain';
import { UsersActions } from 'stores/users/UsersStore';
import { type ThemeInterface } from 'theme';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { DataTable, Spinner, PaginatedList } from 'components/common';
import { Col, Row } from 'components/graylog';

import UserOverviewItem from './UserOverviewItem';
import UsersFilter from './UsersFilter';
import ClientAddressHead from './ClientAddressHead';
import SystemAdministrator from './SystemAdministratorOverview';

const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
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

const _onPageChange = (loadUsers, setLoading) => (page, perPage) => {
  setLoading(true);

  return loadUsers(page, perPage).then(() => setLoading(false));
};

const UsersOverview = () => {
  const currentUser = useContext(CurrentUserContext);
  const [loading, setLoading] = useState(false);
  const [paginatedUsers, setPaginatedUsers] = useState({ adminUser: undefined, list: undefined, pagination: DEFAULT_PAGINATION });
  const { adminUser, list: users, pagination: { page, perPage, query, total } } = paginatedUsers;
  const _isActiveItem = (user) => currentUser?.username === user.username;
  const _userOverviewItem = (user) => <UserOverviewItem user={user} isActive={_isActiveItem(user)} />;

  const _loadUsers = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return UsersDomain.loadUsersPaginated(newPage, newPerPage, newQuery)
      .then((newPaginatedUsers) => newPaginatedUsers && setPaginatedUsers(newPaginatedUsers));
  };

  const _handleSearch = (newQuery) => _loadUsers(DEFAULT_PAGINATION.page, undefined, newQuery);

  useEffect(() => {
    _loadUsers(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query);

    const unlistenDeleteUser = UsersActions.delete.completed.listen(() => {
      _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    return () => {
      unlistenDeleteUser();
    };
  }, []);

  if (!users) {
    return <Spinner />;
  }

  return (
    <Container>
      {adminUser && (
        <SystemAdministrator adminUser={adminUser}
                             dataRowFormatter={_userOverviewItem}
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
          <StyledPaginatedList onChange={_onPageChange(_loadUsers, setLoading)} totalItems={total} activePage={page}>
            <DataTable id="users-overview"
                       className="table-hover"
                       rowClassName="no-bm"
                       headers={TABLE_HEADERS}
                       headerCellFormatter={_headerCellFormatter}
                       sortByKey="fullName"
                       rows={users.toJS()}
                       customFilter={<UsersFilter onSearch={_handleSearch} onReset={() => _handleSearch('')} />}
                       dataRowFormatter={_userOverviewItem}
                       filterKeys={[]}
                       filterLabel="Filter Users" />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default UsersOverview;
