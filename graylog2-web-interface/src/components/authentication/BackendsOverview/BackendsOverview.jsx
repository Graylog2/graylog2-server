// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';
import { DataTable, PaginatedList, Spinner } from 'components/common';
import { Col, Row } from 'components/graylog';

import BackendsFilter from './BackendsFilter';
import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Description', 'Actions'];

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
  const [paginatedBackends, setPaginatedBackends] = useState({ adminUser: undefined, list: undefined, pagination: DEFAULT_PAGINATION, globalConfig: undefined });
  const { list: backends, pagination: { page, perPage, query, total }, globalConfig } = paginatedBackends;
  const [loading, setLoading] = useState();
  const _isActive = (authBackend) => authBackend.id === globalConfig?.activeBackend;

  const _loadBackends = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return AuthenticationDomain.loadBackendsPaginated(newPage, newPerPage, newQuery)
      .then((newPaginatedBackends) => newPaginatedBackends && setPaginatedBackends(newPaginatedBackends));
  };

  const _handleSearch = (newQuery) => _loadBackends(DEFAULT_PAGINATION.page, undefined, newQuery);

  useEffect(() => {
    _loadBackends(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query);

    const unlistenDisableBackend = AuthenticationActions.disableUser.completed.listen(() => {
      _loadBackends(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenEnableBackend = AuthenticationActions.enableUser.completed.listen(() => {
      _loadBackends(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenDeleteBackend = AuthenticationActions.delete.completed.listen(() => {
      _loadBackends(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    return () => {
      unlistenDisableBackend();
      unlistenEnableBackend();
      unlistenDeleteBackend();
    };
  }, []);

  if (!backends) {
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
                       <BackendsOverviewItem authenticationBackend={authBackend} isActive={_isActive(authBackend)} />
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
