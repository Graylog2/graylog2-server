// @flow strict
import * as React from 'react';
import { useEffect, useState, useCallback } from 'react';
import styled, { css } from 'styled-components';

import type { PaginatedRoles } from 'actions/roles/AuthzRolesActions';
import type { PaginatedBackends } from 'actions/authentication/AuthenticationActions';
import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import { Col, Row } from 'components/graylog';

import BackendsFilter from './BackendsFilter';
import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Default Roles'];

const DEFAULT_PAGINATION = {
  page: 1,
  perPage: 10,
  query: '',
};

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const _headerCellFormatter = (header) => {
  switch (header.toLocaleLowerCase()) {
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

const _updateListOnUserDisable = (refreshOverview) => AuthenticationActions.disableUser.completed.listen(refreshOverview);
const _updateListOnUserEnable = (refreshOverview) => AuthenticationActions.enableUser.completed.listen(refreshOverview);
const _updateListOnBackendDelete = (refreshOverview) => AuthenticationActions.delete.completed.listen(refreshOverview);
const _updateListOnBackendActivation = (refreshOverview) => AuthenticationActions.setActiveBackend.completed.listen(refreshOverview);

const BackendsOverview = () => {
  const [loading, setLoading] = useState();
  const [paginatedRoles, setPaginatedRoles] = useState<?PaginatedRoles>();
  const [paginatedBackends, setPaginatedBackends] = useState<?PaginatedBackends>();
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const { list: backends, context } = paginatedBackends || {};
  const { page, perPage, query } = pagination;

  const _refreshOverview = useCallback(() => setPagination({ page: DEFAULT_PAGINATION.page, perPage, query }), [perPage, query]);

  useEffect(() => _loadRoles(setPaginatedRoles), []);
  useEffect(() => _loadBackends(pagination, setLoading, setPaginatedBackends), [pagination]);
  useEffect(() => _updateListOnUserDisable(_refreshOverview), [_refreshOverview]);
  useEffect(() => _updateListOnUserEnable(_refreshOverview), [_refreshOverview]);
  useEffect(() => _updateListOnBackendDelete(_refreshOverview), [_refreshOverview]);
  useEffect(() => _updateListOnBackendActivation(_refreshOverview), [_refreshOverview]);

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
        <PaginatedList onChange={(newPage, newPerPage) => setPagination({ ...pagination, page: newPage, perPage: newPerPage })}
                       totalItems={paginatedBackends.pagination.total}
                       activePage={page}>
          <DataTable className="table-hover"
                     customFilter={<BackendsFilter onSearch={(newQuery) => setPagination({ ...pagination, query: newQuery, page: DEFAULT_PAGINATION.page })} />}
                     dataRowFormatter={(authBackend) => (
                       <BackendsOverviewItem authenticationBackend={authBackend} isActive={authBackend.id === context?.activeBackend?.id} roles={paginatedRoles.list} />
                     )}
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
