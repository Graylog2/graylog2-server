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
// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled, { StyledComponent } from 'styled-components';

import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import { AuthzRolesActions } from 'stores/roles/AuthzRolesStore';
import { ThemeInterface } from 'theme';
import { DataTable, Spinner, PaginatedList, EmptyResult } from 'components/common';
import { Col, Row } from 'components/graylog';

import RolesOverviewItem from './RolesOverviewItem';
import RolesFilter from './RolesFilter';

const TABLE_HEADERS = ['Name', 'Description', 'Users', 'Actions'];
const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

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

const _updateListOnRoleDelete = (perPage, query, setPagination) => AuthzRolesActions.delete.completed.listen(() => setPagination({ page: DEFAULT_PAGINATION.page, perPage, query }));

const RolesOverview = () => {
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined | null>();
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: roles } = paginatedRoles || {};
  const { page, perPage, query } = pagination;

  useEffect(() => _loadRoles(pagination, setLoading, setPaginatedRoles), [pagination]);
  useEffect(() => _updateListOnRoleDelete(perPage, query, setPagination), [perPage, query]);

  if (!paginatedRoles) {
    return <Spinner />;
  }

  const searchFilter = <RolesFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })} />;
  const _rolesOverviewItem = (role) => <RolesOverviewItem role={role} users={paginatedRoles?.context?.users[role.id]} />;

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
