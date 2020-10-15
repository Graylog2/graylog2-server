// @flow strict
import * as React from 'react';

import UserOverview from 'logic/users/UserOverview';
import { DataTable } from 'components/common';
import { Col, Row } from 'components/graylog';

type Props = {
  adminUser: UserOverview,
  dataRowFormatter: (user: UserOverview) => React.Node,
  headerCellFormatter: (header: string) => React.Node,
  headers: Array<string>,
};

const SystemAdministratorOverview = ({ adminUser, dataRowFormatter, headers, headerCellFormatter }: Props) => (
  <Row className="content">
    <Col xs={12}>
      <h2>System Administrator</h2>
      <p className="description">
        The system administrator can only be edited in the graylog configuration file.
      </p>
      <DataTable id="users-overview"
                 className="table-hover"
                 headers={headers}
                 headerCellFormatter={headerCellFormatter}
                 sortByKey="fullName"
                 rows={[adminUser]}
                 dataRowFormatter={dataRowFormatter}
                 filterKeys={[]} />
    </Col>
  </Row>
);

export default SystemAdministratorOverview;
