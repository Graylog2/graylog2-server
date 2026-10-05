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
import { useState, useCallback } from 'react';
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { AUTHZ_ROLES_QUERY_KEY } from 'hooks/useAuthzRoles';
import { DataTable, Spinner, PaginatedList, NoSearchResult } from 'components/common';
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

const LoadingSpinner = styled(Spinner)(
  ({ theme }) => `
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`,
);

const StyledPaginatedList = styled(PaginatedList)`
  .pagination {
    margin: 0;
  }
`;

const _headerCellFormatter = (header) => {
  switch (header.toLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const getUseTeamMembersHook = () => {
  const defaultHook = () => ({ loading: false, users: [] });
  const teamsPlugin = PluginStore.exports('teams');

  return teamsPlugin?.[0]?.useTeamMembersByRole || defaultHook;
};

const RolesOverview = () => {
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const [query, setQuery] = useState('');
  const queryClient = useQueryClient();
  const useTeamMembersByRole = getUseTeamMembersHook();
  const teamMembersByRole = useTeamMembersByRole();

  const { data: paginatedRoles, isFetching } = useQuery({
    queryKey: [...AUTHZ_ROLES_QUERY_KEY, 'paginated', { page, perPage, query }],
    queryFn: () => AuthzRolesDomain.loadRolesPaginated({ page, perPage, query }),
    placeholderData: keepPreviousData,
  });

  const { list: roles } = paginatedRoles || {};

  const handleSearch = (newQuery: string) => {
    resetPage();
    setQuery(newQuery);
  };

  const onRoleDeleted = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: AUTHZ_ROLES_QUERY_KEY });
    resetPage();
  }, [queryClient, resetPage]);

  const _rolesOverviewItem = useCallback(
    (role) => {
      const { id: roleId } = role;
      const roleUsers = paginatedRoles?.context.users[roleId];
      const users = teamMembersByRole.users[roleId]
        ? [...teamMembersByRole.users[roleId], ...roleUsers]
        : paginatedRoles?.context?.users[roleId];

      return <RolesOverviewItem role={role} users={users} onDeleted={onRoleDeleted} />;
    },
    [teamMembersByRole, paginatedRoles?.context, onRoleDeleted],
  );

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
            {isFetching && <LoadingSpinner text="" delay={0} />}
          </Header>
          <p className="description">Found {paginatedRoles.pagination.total} roles on the system.</p>
          <StyledPaginatedList totalItems={paginatedRoles.pagination.total}>
            <DataTable
              id="roles-overview"
              rowClassName="no-bm"
              headers={TABLE_HEADERS}
              headerCellFormatter={_headerCellFormatter}
              sortByKey="name"
              rows={roles.toJS()}
              noDataText={<NoSearchResult>No roles have been found.</NoSearchResult>}
              customFilter={searchFilter}
              dataRowFormatter={_rolesOverviewItem}
              filterKeys={[]}
              filterLabel="Filter Roles"
            />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default RolesOverview;
