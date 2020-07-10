// @flow strict
import * as React from 'react';
import { useEffect, useContext } from 'react';
import styled from 'styled-components';

import { useStore } from 'stores/connect';
import UsersStore from 'stores/users/UsersStore';
import UsersActions from 'actions/users/UsersActions';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { DataTable, Spinner, PaginatedList, SearchForm } from 'components/common';
import { Col, Row } from 'components/graylog';

import UserOverviewItem from './UserOverviewItem';
import ClientAddressHead from './ClientAddressHead';

const TableWrapper = styled.div`
  .data-table {
    overflow-x: visible;
  }
`;

const Filter = ({ perPage }: { perPage: number }) => {
  const handleSearch = (newQuery, resetLoading) => UsersActions.searchPaginated(1, perPage, newQuery).then(resetLoading);
  const handleReset = () => UsersActions.searchPaginated(1, perPage, '');

  return (
    <SearchForm onSearch={handleSearch}
                onReset={handleReset}
                useLoadingState
                topMargin={0} />
  );
};

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

const _onPageChange = (query) => (page, perPage) => UsersActions.searchPaginated(page, perPage, query);

const UsersOverview = () => {
  const {
    paginatedList: {
      adminUser,
      list: users,
      pagination: { page, perPage, query, total },
    },
  } = useStore(UsersStore);
  const roles = [];
  const currentUser = useContext(CurrentUserContext);
  const headers = ['', 'Full name', 'Username', 'E-Mail Address', 'Client Address', 'Role', 'Actions'];
  const _isActiveItem = (user) => currentUser?.username === user.username;
  const _userOverviewItem = (user) => <UserOverviewItem user={user} roles={roles} isActive={_isActiveItem(user)} />;

  useEffect(() => {
    UsersActions.searchPaginated(page, perPage, query);

    const unlistenDeleteUser = UsersActions.deleteUser.completed.listen(() => {
      UsersActions.searchPaginated(page, perPage, query);
    });

    return () => {
      unlistenDeleteUser();
    };
  }, []);

  if (!users || !roles) {
    return <Spinner />;
  }

  return (
    <>
      {adminUser && (
        <Row className="content">
          <Col xs={12}>
            <h2>System Administrator</h2>
            <p className="description">
              The system administrator can only be edit in the graylog configuration file.
            </p>
            <TableWrapper>
              <DataTable id="users-overview"
                         className="table-hover"
                         headers={headers}
                         headerCellFormatter={_headerCellFormatter}
                         sortByKey="fullName"
                         rows={[adminUser]}
                         filterBy="role"
                         dataRowFormatter={_userOverviewItem}
                         filterKeys={[]}
                         filterLabel="Filter Users" />
            </TableWrapper>
          </Col>
        </Row>
      )}
      <Row className="content">
        <Col xs={12}>
          <h2>Users</h2>
          <p className="description">
            Found {total} registered users on the system.
          </p>
          <PaginatedList onChange={_onPageChange(query)} totalItems={total} activePage={page}>
            <TableWrapper>
              <DataTable id="users-overview"
                         className="table-hover"
                         headers={headers}
                         headerCellFormatter={_headerCellFormatter}
                         sortByKey="fullName"
                         rows={users.toJS()}
                         filterBy="role"
                         customFilter={<Filter perPage={perPage} />}
                         dataRowFormatter={_userOverviewItem}
                         filterKeys={[]}
                         filterLabel="Filter Users" />
            </TableWrapper>
          </PaginatedList>
        </Col>
      </Row>
    </>
  );
};

export default UsersOverview;
