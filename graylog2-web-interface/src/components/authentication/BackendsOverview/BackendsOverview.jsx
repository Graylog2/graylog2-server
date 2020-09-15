// @flow strict
import * as React from 'react';
import { useEffect } from 'react';

// import AuthenticationDomain from 'domainActions/authentication/AuthenticationDomain';
import AuthenticationActions, { type PaginatedBackends } from 'actions/authentication/AuthenticationActions';
import { Col, Row } from 'components/graylog';
import { DataTable, PaginatedList } from 'components/common';

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

type Props = {
  paginatedAuthBackends: PaginatedBackends,
};

const _headerCellFormatter = (header) => {
  switch (header.toLocaleLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const BackendsOverview = ({ paginatedAuthBackends }: Props) => {
  // const [paginatedBackends, setPaginatedBackends] = useState({ adminUser: undefined, list: undefined, pagination: DEFAULT_PAGINATION });
  // const { list: users, pagination: { page, perPage, query, total } } = paginatedBackends;

  const backends = paginatedAuthBackends.list;
  const _isActive = (authBackend) => authBackend.id === paginatedAuthBackends.globalConfig.activeBackend;

  // const _loadUsers = () => {
  // return AuthenticationDomain.loadUsersPaginated(newPage, newPerPage, newQuery)
  //   .then((newPaginatedBackends) => newPaginatedBackends && setPaginatedBackends(newPaginatedBackends));
  // };

  useEffect(() => {
    const unlistenDisableBackend = AuthenticationActions.disableUser.completed.listen(() => {
      // _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenEnableBackend = AuthenticationActions.enableUser.completed.listen(() => {
      // _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    const unlistenDeleteBackend = AuthenticationActions.enableUser.completed.listen(() => {
      // _loadUsers(DEFAULT_PAGINATION.page, undefined, DEFAULT_PAGINATION.query);
    });

    return () => {
      unlistenDisableBackend();
      unlistenEnableBackend();
      unlistenDeleteBackend();
    };
  }, []);

  return (
    <Row className="content">
      <Col xs={12}>
        <h2>Authentication Services</h2>
        <p className="description">
          Found {backends.size} configured authentication services on the system.
        </p>
        <PaginatedList onChange={() => {}} totalItems={5} activePage={1}>
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
