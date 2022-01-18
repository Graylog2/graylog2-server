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
import styled from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import { Spinner, PaginatedList } from 'components/common';
import { Col, Row } from 'components/bootstrap';

import RolesTable from './RolesTable';
import RolesFilter from './RolesFilter';

const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

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

const _loadRoles = (pagination, setLoading, setPaginatedRoles) => {
  setLoading(true);

  AuthzRolesDomain.loadRolesPaginated(pagination).then((paginatedRoles) => {
    setPaginatedRoles(paginatedRoles);
    setLoading(false);
  });
};

const _updateListOnRoleDelete = (perPage, query, setPagination) => AuthzRolesActions.delete.completed.listen(() => setPagination({ page: DEFAULT_PAGINATION.page, perPage, query }));

const _headerCellFormatter = (header) => {
  // eslint-disable-next-line react/destructuring-assignment
  switch (header.toLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const RolesOverview = () => {
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined | null>();
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: roles } = paginatedRoles || {};
  const { page, perPage, query } = pagination;
  const teamsPlugin = PluginStore.exports('teams');
  const RolesTableWithTeamMembers = teamsPlugin?.[0]?.RolesTableWithTeamMembers;

  useEffect(() => _loadRoles(pagination, setLoading, setPaginatedRoles), [pagination]);
  useEffect(() => _updateListOnRoleDelete(perPage, query, setPagination), [perPage, query]);

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const searchFilter = <RolesFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: 1 })} />;

  const RolesTableComponent = RolesTableWithTeamMembers || RolesTable;

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
          <StyledPaginatedList activePage={page}
                               totalItems={paginatedRoles.pagination.total}
                               onChange={(newPage, newPerPage) => setPagination({ ...pagination, page: newPage, perPage: newPerPage })}>
            <RolesTableComponent roles={roles}
                                 setPagination={setPagination}
                                 searchFilter={searchFilter}
                                 pagination={pagination}
                                 headerCellFormatter={_headerCellFormatter}
                                 paginatedRoles={paginatedRoles} />
          </StyledPaginatedList>
        </Col>
      </Row>
    </Container>
  );
};

export default RolesOverview;
