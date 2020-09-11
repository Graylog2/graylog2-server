// @flow strict
import * as React from 'react';

import type { PaginatedBackends } from 'actions/authentication/AuthenticationActions';
import { Col, Row } from 'components/graylog';
import { DataTable } from 'components/common';

import BackendsOverviewItem from './BackendsOverviewItem';

const TABLE_HEADERS = ['Title', 'Description', 'Actions'];

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
  const backends = paginatedAuthBackends.list;
  const _isActive = (authBackend) => authBackend.id === paginatedAuthBackends.globalConfig.activeBackend;

  return (
    <Row className="content">
      <Col xs={12}>
        <h2>Users</h2>
        <p className="description">
          Found {backends.size} configured authentication services on the system.
        </p>

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
                   filterKeys={[]}
                   filterLabel="Filter configured services" />

      </Col>
    </Row>
  );
};

export default BackendsOverview;
