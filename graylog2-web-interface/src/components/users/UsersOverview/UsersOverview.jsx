// @flow strict
import * as React from 'react';
import { useEffect, useContext, useState } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { UsersActions, UsersStore } from 'stores/users/UsersStore';
import { type ThemeInterface } from 'theme';
import { useStore } from 'stores/connect';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { DataTable, Spinner, PaginatedList } from 'components/common';
import { Col, Row } from 'components/graylog';

import UserOverviewItem from './UserOverviewItem';
import UsersFilter from './UsersFilter';
import ClientAddressHead from './ClientAddressHead';
import SystemAdministrator from './SystemAdministratorOverview';

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

const _onPageChange = (query, setLoading) => (page, perPage) => {
  setLoading(true);

  return UsersActions.searchPaginated(page, perPage, query).then(() => setLoading(false));
};

const UsersOverview = () => {
  const {
    paginatedList: {
      adminUser,
      list: users,
      pagination: { page, perPage, query, total },
    },
  } = useStore(UsersStore);
  const [loading, setLoading] = useState(false);
  const currentUser = useContext(CurrentUserContext);
  const _isActiveItem = (user) => currentUser?.username === user.username;
  const _userOverviewItem = (user) => <UserOverviewItem user={user} isActive={_isActiveItem(user)} />;

  useEffect(() => {
    UsersActions.searchPaginated(page, perPage, query);

    const unlistenDeleteUser = UsersActions.deleteUser.completed.listen(() => {
      UsersActions.searchPaginated(page, perPage, query);
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
          <StyledPaginatedList onChange={_onPageChange(query, setLoading)} totalItems={total} activePage={page}>
            <DataTable id="users-overview"
                       className="table-hover"
                       rowClassName="no-bm"
                       headers={TABLE_HEADERS}
                       headerCellFormatter={_headerCellFormatter}
                       sortByKey="fullName"
                       rows={users.toJS()}
                       customFilter={<UsersFilter perPage={perPage} />}
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
