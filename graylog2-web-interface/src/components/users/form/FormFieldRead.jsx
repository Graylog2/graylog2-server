// @flow strict
import * as React from 'react';
import styled from 'styled-components';

import { Col, Row } from 'components/graylog';

type Props = {
  label: React.Node,
  value: React.Node,
};

const ValueCol = styled(Col)`
  padding-top: 7px;
`;

const LabelCol = styled(ValueCol)`
  text-align: right;
`;

const FormFieldRead = ({ label, value }: Props) => (
  <Row>
    <LabelCol sm={3}>
      <b>{label}</b>
    </LabelCol>
    <ValueCol sm={9}>
      {value}
    </ValueCol>
  </Row>
);

export default FormFieldRead;
