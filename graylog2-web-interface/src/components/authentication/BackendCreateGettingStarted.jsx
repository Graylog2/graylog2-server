// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { EmptyEntity } from 'components/common';

import BackendCreateSelect from './BackendCreateSelect';

type Props = {
  title?: string,
};

const BackendCreateGettingStarted = ({ title }: Props) => (
  <Row className="content">
    <Col md={6} mdOffset={3}>
      <EmptyEntity title={title}>
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

BackendCreateGettingStarted.defaultProps = {
  title: undefined,
};

export default BackendCreateGettingStarted;
