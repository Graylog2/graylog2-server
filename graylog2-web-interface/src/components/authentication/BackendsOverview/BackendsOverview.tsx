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
import styled, { css } from 'styled-components';

import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import type { PaginatedBackends } from 'stores/authentication/AuthenticationStore';
import { AuthenticationActions } from 'stores/authentication/AuthenticationStore';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import { Col, Row } from 'components/bootstrap';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import type AuthenticationBackend from 'logic/authentication/AuthenticationBackend';

import BackendsFilter from './BackendsFilter';
import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Description', 'Default Roles', 'Actions'];

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const _headerCellFormatter = (header) => {
  switch (header.toLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const _loadRoles = (setPaginatedRoles) => {
  const getUnlimited = { page: 1, perPage: 0, query: '' };

  AuthzRolesDomain.loadRolesPaginated(getUnlimited).then(setPaginatedRoles);
};

const _loadBackends = (pagination, setLoading, setPaginatedBackends) => {
  setLoading(true);

  AuthenticationDomain.loadBackendsPaginated(pagination).then((paginatedUsers) => {
    setPaginatedBackends(paginatedUsers);
    setLoading(false);
  });
};

const _updateListOnBackendDelete = (refreshOverview) => AuthenticationActions.delete.completed.listen(refreshOverview);
const _updateListOnBackendActivation = (refreshOverview) => AuthenticationActions.setActiveBackend.completed.listen(refreshOverview);

const _backendsOverviewItem = (authBackend: AuthenticationBackend, context: { activeBackend: AuthenticationBackend }, paginatedRoles: PaginatedRoles) => (
  <BackendsOverviewItem authenticationBackend={authBackend} isActive={authBackend.id === context?.activeBackend?.id} roles={paginatedRoles.list} />
);

const BackendsOverview = () => {
  const { page, pageSize: perPage, resetPage } = usePaginationQueryParameter();
  const [loading, setLoading] = useState();
  const [paginatedRoles, setPaginatedRoles] = useState<PaginatedRoles | undefined>();
  const [paginatedBackends, setPaginatedBackends] = useState<PaginatedBackends | undefined>();
  const [query, setQuery] = useState('');
  const { list: backends, context } = paginatedBackends || {};

  const _refreshOverview = useCallback(() => resetPage(), [resetPage]);

  useEffect(() => _loadRoles(setPaginatedRoles), []);
  useEffect(() => _loadBackends({ query, page, perPage }, setLoading, setPaginatedBackends), [query, page, perPage]);
  useEffect(() => _updateListOnBackendDelete(_refreshOverview), [_refreshOverview]);
  useEffect(() => _updateListOnBackendActivation(_refreshOverview), [_refreshOverview]);

  const onSearch = (newQuery: string) => {
    resetPage();
    setQuery(newQuery);
  };

  if (!paginatedBackends || !paginatedRoles) {
    return <Spinner />;
  }

  return (
    <Row className="content">
      <Col xs={12}>
        <h2>Configured Authentication Services</h2>
        <Header>
          {loading && <LoadingSpinner text="" delay={0} />}
        </Header>
        <p className="description">
          Found {paginatedBackends.pagination.total} configured authentication services on the system.
        </p>
        <PaginatedList totalItems={paginatedBackends.pagination.total}>
          <DataTable className="table-hover"
                     customFilter={<BackendsFilter onSearch={onSearch} />}
                     dataRowFormatter={(authBackend) => _backendsOverviewItem(authBackend, context, paginatedRoles)}
                     filterKeys={[]}
                     filterLabel="Filter services"
                     headerCellFormatter={_headerCellFormatter}
                     headers={TABLE_HEADERS}
                     id="auth-backends-overview"
                     rowClassName="no-bm"
                     rows={backends.toJS()}
                     sortByKey="title" />
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default BackendsOverview;
