// @flow strict
import * as React from 'react';

import type { PaginatedServices } from 'actions/authentication/AuthenticationActions';
import { Col, Row } from 'components/graylog';
import { DataTable } from 'components/common';

import AuthenticationServicesOverviewItem from './AuthenticationServicesOverviewItem';

const TABLE_HEADERS = ['Title', 'Description', 'Actions'];

type Props = {
  paginatedAuthServices: PaginatedServices,
};

const _headerCellFormatter = (header) => {
  switch (header.toLocaleLowerCase()) {
    case 'actions':
      return <th className="actions text-right">{header}</th>;
    default:
      return <th>{header}</th>;
  }
};

const AuthenticationServicesOverview = ({ paginatedAuthServices }: Props) => {
  const services = paginatedAuthServices.list;
  const _isActive = (authService) => authService.id === paginatedAuthServices.globalConfig.activeBackend;
  const _authServiceOverviewItem = (authService) => <AuthenticationServicesOverviewItem authService={authService} isActive={_isActive(authService)} />;

  return (
    <Row className="content">
      <Col xs={12}>
        <h2>Users</h2>
        <p className="description">
          Found {services.size} configured authenticationservices on the system.
        </p>

        <DataTable id="auth-services-overview"
                   className="table-hover"
                   rowClassName="no-bm"
                   headers={TABLE_HEADERS}
                   headerCellFormatter={_headerCellFormatter}
                   sortByKey="title"
                   rows={services.toJS()}
                   dataRowFormatter={_authServiceOverviewItem}
                   filterKeys={[]}
                   filterLabel="Filter Users" />

      </Col>
    </Row>
  );
};

export default AuthenticationServicesOverview;
