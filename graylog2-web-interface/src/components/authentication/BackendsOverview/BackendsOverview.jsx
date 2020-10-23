// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthzRolesDomain from 'domainActions/roles/AuthzRolesDomain';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import { Col, Row } from 'components/graylog';

import BackendsFilter from './BackendsFilter';
import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Default Roles'];

const DEFAULT_PAGINATION = {
  count: undefined,
  page: 1,
  perPage: 10,
  query: '',
  total: undefined,
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

const _onPageChange = (loadBackends, setLoading) => (page, perPage) => {
  setLoading(true);

  return loadBackends(page, perPage).then(() => setLoading(false));
};

const BackendsOverview = () => {
  const [paginatedBackends, setPaginatedBackends] = useState({ adminUser: undefined, list: undefined, pagination: DEFAULT_PAGINATION, context: undefined });
  const { list: backends, pagination: { page, perPage, query, total }, context } = paginatedBackends;
  const [{ list: roles }, setPaginatedRoles] = useState({ list: Immutable.List() });
  const [loading, setLoading] = useState();
  const _isActive = (authBackend) => authBackend.id === context?.activeBackend?.id;

  const _loadBackends = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return AuthenticationDomain.loadBackendsPaginated(newPage, newPerPage, newQuery).then(setPaginatedBackends);
  };

  const _handleSearch = (newQuery) => _loadBackends(DEFAULT_PAGINATION.page, undefined, newQuery);
  const _refreshOverview = () => _loadBackends(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);

  useEffect(() => {
    const getUnlimited = [1, 0, ''];
    AuthzRolesDomain.loadRolesPaginated(...getUnlimited).then(setPaginatedRoles);
    _loadBackends(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query);

    const unlistenDisableBackend = AuthenticationActions.disableUser.completed.listen(_refreshOverview);
    const unlistenEnableBackend = AuthenticationActions.enableUser.completed.listen(_refreshOverview);
    const unlistenDeleteBackend = AuthenticationActions.delete.completed.listen(_refreshOverview);
    const unlistenSetActivateBackend = AuthenticationActions.setActiveBackend.completed.listen(_refreshOverview);

    return () => {
      unlistenDisableBackend();
      unlistenEnableBackend();
      unlistenSetActivateBackend();
      unlistenDeleteBackend();
    };
  }, []);

  if (!backends || !roles) {
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
          Found {backends.size} configured authentication services on the system.
        </p>
        <PaginatedList onChange={_onPageChange(_loadBackends, setLoading)} totalItems={total} activePage={1}>
          <DataTable className="table-hover"
                     customFilter={<BackendsFilter onSearch={_handleSearch} onReset={() => _handleSearch('')} />}
                     dataRowFormatter={(authBackend) => (
                       <BackendsOverviewItem authenticationBackend={authBackend} isActive={_isActive(authBackend)} roles={roles} />
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
