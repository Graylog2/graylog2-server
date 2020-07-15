// @flow strict
import * as React from 'react';

import { Row, Col } from 'components/graylog';
import { TextLabel } from 'components/common';

type Props = {
  label: string,
  value: React.Node,
};

const ShowConfigValue = ({ label, value }: Props) => (
  <Row>
    <Col xs={3}>
      <TextLabel>{label}</TextLabel>
    </Col>
    <Col xs={9}>
      {value ?? '-'}
    </Col>
  </Row>
);

export default ShowConfigValue;
