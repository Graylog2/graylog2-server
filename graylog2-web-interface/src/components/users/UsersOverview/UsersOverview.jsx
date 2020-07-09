// @flow strict
import * as React from 'react';
import { useEffect, useContext } from 'react';
import styled from 'styled-components';

import { useStore } from 'stores/connect';
import UsersStore from 'stores/users/UsersStore';
import UsersActions from 'actions/users/UsersActions';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { DataTable, Spinner } from 'components/common';
import { Col, Row } from 'components/graylog';

import UserOverviewItem from './UserOverviewItem';
import ClientAddressHead from './ClientAddressHead';

const TableWrapper = styled.div`
  .data-table {
    overflow-x: visible;
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

const UsersOverview = () => {
  const { list: users } = useStore(UsersStore);
  const roles = [];
  const currentUser = useContext(CurrentUserContext);
  const filterKeys = ['username', 'fullName', 'email', 'clientAddress'];
  const headers = ['', 'Username', 'Full name', 'Email Address', 'Client Address', 'Role', 'Actions'];
  const _isActiveItem = (user) => currentUser?.username === user.username;
  const _userOverviewItem = (user) => <UserOverviewItem user={user} roles={roles} isActive={_isActiveItem(user)} />;

  useEffect(() => {
    UsersActions.loadUsers();

    const unlistenDeleteUser = UsersActions.deleteUser.completed.listen(() => {
      UsersActions.loadUsers();
    });

    return () => {
      unlistenDeleteUser();
    };
  }, []);

  if (!users || !roles) {
    return <Spinner />;
  }

  return (
    <Row className="content">
      <Col xs={12}>
        <TableWrapper>
          <DataTable id="users-overview"
                     className="table-hover"
                     headers={headers}
                     headerCellFormatter={_headerCellFormatter}
                     sortByKey="fullName"
                     rows={users.toJS()}
                     filterBy="role"
                     // filterSuggestions={roles}
                     dataRowFormatter={_userOverviewItem}
                     filterKeys={filterKeys}
                     filterLabel="Filter Users" />
        </TableWrapper>
      </Col>
    </Row>
  );
};

export default UsersOverview;
