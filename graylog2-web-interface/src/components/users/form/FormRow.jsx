// @flow strict
import * as React from 'react';

import { Col, Row } from 'components/graylog';

type Props = {
  label: React.Node,
  children: React.Node,
};

const FormRow = ({ label, children }: Props) => (
  <Row>
    <Col xs={3}>
      {label}
    </Col>
    <Col xs={9}>
      {children}
    </Col>
  </Row>
);

export default FormRow;
