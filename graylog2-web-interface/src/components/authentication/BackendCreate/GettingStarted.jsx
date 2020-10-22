// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { EmptyEntity } from 'components/common';

import ServiceSelect from './ServiceSelect';

type Props = {
  title?: string,
};

const GettingStarted = ({ title }: Props) => (
  <Row className="content">
    <Col md={6} mdOffset={3}>
      <EmptyEntity title={title}>
        <p>
          Beside the built-in authentication mechanisms like its internal user database or LDAP/Active Directory,
          authentication services can also be extended by plugins to support other authentication mechanisms.
          Select an authentication service to setup a new one.
        </p>
        <ServiceSelect />
      </EmptyEntity>
    </Col>
  </Row>
);

GettingStarted.defaultProps = {
  title: undefined,
};

export default GettingStarted;
