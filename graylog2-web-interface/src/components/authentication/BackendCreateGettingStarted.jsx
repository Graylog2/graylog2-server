// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { EmptyEntity } from 'components/common';

import BackendCreateSelect from './BackendCreateSelect';

const BackendCreateGettingStarted = () => (
  <Row className="content">
    <Col md={6} mdOffset={3}>
      <EmptyEntity>
        <p>
          Beside the builtin authentication mechanisms like its internal user database or LDAP/Active Directory,
          authentication services can also be extended by plugins to support other authentication mechanisms.
          Select an authentication services to setup a new one.
        </p>
        <BackendCreateSelect />
      </EmptyEntity>
    </Col>
  </Row>
);

export default BackendCreateGettingStarted;
