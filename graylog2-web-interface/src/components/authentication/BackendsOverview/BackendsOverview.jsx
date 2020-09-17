// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled from 'styled-components';

import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthenticationActions from 'actions/authentication/AuthenticationActions';
import { Col, Row } from 'components/graylog';
import { DataTable, PaginatedList, Spinner } from 'components/common';

import BackendsFilter from './BackendsFilter';
import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Description', 'Actions'];

const DEFAULT_PAGINATION = {
  count: undefined,
  total: undefined,
  page: 1,
  perPage: 10,
  query: '',
};

const Header = styled.div`
  display: flex;
  align-items: center;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => `
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
  console.log(paginatedBackends);
  const { list: backends, pagination: { page, perPage, query, total }, globalConfig } = paginatedBackends;
  const [loading, setLoading] = useState();
  const _isActive = (authBackend) => authBackend.id === globalConfig?.activeBackend;

  const _loadBackends = (newPage = page, newPerPage = perPage, newQuery = query) => {
    return AuthenticationDomain.loadBackendsPaginated(newPage, newPerPage, newQuery)
      .then((newPaginatedBackends) => newPaginatedBackends && setPaginatedBackends(newPaginatedBackends));
  };

  useEffect(() => {
    _loadBackends(DEFAULT_PAGINATION.page, DEFAULT_PAGINATION.perPage, DEFAULT_PAGINATION.query);

    const unlistenDisableBackend = AuthenticationActions.disableUser.completed.listen(() => {
      _loadBackends(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenEnableBackend = AuthenticationActions.enableUser.completed.listen(() => {
      _loadBackends(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenDeleteBackend = AuthenticationActions.enableUser.completed.listen(() => {
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
          <DataTable id="auth-backends-overview"
                     className="table-hover"
                     rowClassName="no-bm"
                     headers={TABLE_HEADERS}
                     headerCellFormatter={_headerCellFormatter}
                     sortByKey="title"
                     rows={backends.toJS()}
                     dataRowFormatter={(authBackend) => (
                       <BackendsOverviewItem authenticationBackend={authBackend} isActive={_isActive(authBackend)} />
                     )}
                     customFilter={<BackendsFilter onSearch={() => Promise.resolve()} onReset={() => Promise.resolve()} />}
                     filterKeys={[]}
                     filterLabel="Filter services" />
        </PaginatedList>
      </Col>
    </Row>
  );
};

export default BackendsOverview;
