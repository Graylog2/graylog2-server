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
import { useEffect, useState, useCallback } from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import { DataTable, Spinner, PaginatedList, EmptyResult } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';

import RolesOverviewItem from './RolesOverviewItem';
import RolesFilter from './RolesFilter';

const TABLE_HEADERS = ['Name', 'Description', 'Users', 'Actions'];

const Container = styled.div`
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
  // eslint-disable-next-line react/destructuring-assignment
  switch (header.toLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const _loadRoles = (pagination, setLoading, setPaginatedRoles) => {
  setLoading(true);

  AuthzRolesDomain.loadRolesPaginated(pagination).then((paginatedRoles) => {
    setPaginatedRoles(paginatedRoles);
    setLoading(false);
  });
};

const _updateListOnRoleDelete = (pagination, setLoading, setPaginatedRoles, callback: () => void) => AuthzRolesActions.delete.completed.listen(() => {
  _loadRoles(pagination, setLoading, setPaginatedRoles);
  callback();
});

const getUseTeamMembersHook = () => {
  const defaultHook = () => ({ loading: false, users: [] });
  const teamsPlugin = PluginStore.exports('teams');

  return teamsPlugin?.[0]?.useTeamMembersByRole || defaultHook;
};

const RolesOverview = () => {
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined | null>();
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState('');
  const { list: roles } = paginatedRoles || {};
  const useTeamMembersByRole = getUseTeamMembersHook();
  const teamMembersByRole = useTeamMembersByRole();

  useEffect(() => _loadRoles({ page, perPage, query }, setLoading, setPaginatedRoles), [page, perPage, query]);
  useEffect(() => _updateListOnRoleDelete({ page, perPage, query }, setLoading, setPaginatedRoles, resetPage), [page, perPage, query, resetPage]);

  const handleSearch = (newQuery) => {
    resetPage();
    setQuery(newQuery);
  };

  const _rolesOverviewItem = useCallback((role) => {
    const { id: roleId } = role;
    const roleUsers = paginatedRoles?.context.users[roleId];
    const users = teamMembersByRole.users[roleId]
      ? [...teamMembersByRole.users[roleId], ...roleUsers]
      : paginatedRoles?.context?.users[roleId];

    return <RolesOverviewItem role={role} users={users} />;
  }, [teamMembersByRole, paginatedRoles?.context]);

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const searchFilter = <RolesFilter onSearch={handleSearch} />;

  return (
    <Container>
      <Row className="content">
        <Col xs={12}>
          <Header>
            <h2>Roles</h2>
            {loading && <LoadingSpinner text="" delay={0} />}
          </Header>
          <p className="description">
            Found {paginatedRoles.pagination.total} roles on the system.
          </p>
          <StyledPaginatedList totalItems={paginatedRoles.pagination.total}>
            <DataTable id="roles-overview"
                       className="table-hover"
                       rowClassName="no-bm"
                       headers={TABLE_HEADERS}
                       headerCellFormatter={_headerCellFormatter}
                       sortByKey="name"
                       rows={roles.toJS()}
                       noDataText={<EmptyResult>No roles have been found.</EmptyResult>}
                       customFilter={searchFilter}
                       dataRowFormatter={_rolesOverviewItem}
                       filterKeys={[]}
                       filterLabel="Filter Roles" />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default RolesOverview;
